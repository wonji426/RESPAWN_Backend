package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.*;
import com.shop.respawn.dto.order.*;
import com.shop.respawn.dto.user.SellerOrderDetailDto;
import com.shop.respawn.dto.user.SellerOrderDto;
import com.shop.respawn.repository.jpa.*;
import com.shop.respawn.repository.mongo.ItemRepository;
import com.shop.respawn.util.RedisUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.shop.respawn.dto.RefundRequestDetailDto.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BuyerRepository buyerRepository;
    private final ItemRepository itemRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PointLedgerRepository pointLedgerRepository;

    private final LedgerPointService ledgerPointService;
    private final ItemService itemService;
    private final CouponService couponService;

    private final RedisUtil redisUtil;

    /**
     * 임시 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderDetailsDto getOrderDetails(Long orderId, Long buyerId) {
        // 1) 주문 조회 및 권한 검증
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("해당 주문을 조회할 권한이 없습니다.");
        }

        // 2) 기본 배송지 조회
        Address buyerAddress = addressRepository.findByBuyerAndBasicTrue(order.getBuyer()).orElse(null);

        // 3) 주문 아이템 및 관련 Item을 한 번에 조회 (N+1 제거)
        List<OrderItem> orderItems = order.getOrderItems();
        List<String> itemIds = orderItems.stream()
                .map(OrderItem::getItemId)
                .distinct()
                .toList();

        Map<String, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, it -> it));

        // 예외 정책: 연결된 상품이 하나라도 없으면 전체 실패 (정책 변경 가능)
        for (OrderItem oi : orderItems) {
            if (!itemMap.containsKey(oi.getItemId())) {
                throw new RuntimeException("상품을 찾을 수 없습니다: " + oi.getItemId());
            }
        }

        // 4) DTO 리스트 생성 (이미 조회한 itemMap 재사용)
        List<OrderItemDetailDto> orderItemDetails = orderItems.stream()
                .map(oi -> OrderItemDetailDto.from(oi, itemMap.get(oi.getItemId())))
                .toList();

        // 5) 판매자별 1회 배송비 집계 (itemMap 재사용)
        long totalDeliveryFee = getTotalDeliveryFee(orderItems, itemMap);

        // 6) 총 상품금액/총 결제금액 계산
        long totalItemAmount = orderItemDetails.stream()
                .mapToLong(OrderItemDetailDto::getTotalPrice) // orderPrice * count
                .sum();

        long totalAmount = totalItemAmount + totalDeliveryFee;

        // 7) 최종 응답 조립
        return new OrderDetailsDto(order, order.getBuyer(), orderItemDetails, orderItemDetails.size(), totalItemAmount,
                totalDeliveryFee, totalAmount, buyerAddress);
    }


    /**
     * 장바구니 선택 상품으로 주문페이지 이동 (선택된 CartItem을 OrderItem으로 복사만)
     */
    public Long prepareOrderSelectedFromCart(Long buyerId, OrderRequestDto orderRequest) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다: " + buyerId));

        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new RuntimeException("장바구니가 비어있습니다: buyerId=" + buyerId));

        // 1) 선택된 카트 아이템 추출 + 기본 유효성 검증
        List<CartItem> selectedCartItems = cart.getCartItems().stream()
                .filter(cartItem -> orderRequest.getCartItemIds().contains(cartItem.getId()))
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new RuntimeException("선택된 상품이 없습니다: cartItemIds=" + orderRequest.getCartItemIds());
        }

        // 수량 검증
        for (CartItem cartItem : selectedCartItems) {
            if (cartItem.getCount() == null || cartItem.getCount() <= 0) {
                throw new RuntimeException("잘못된 수량의 카트 항목이 있습니다: cartItemId=" + cartItem.getId());
            }
        }

        // 2) 필요한 Item을 한 번에 조회 (N+1 제거)
        List<String> itemIds = selectedCartItems.stream()
                .map(CartItem::getItemId)
                .distinct()
                .toList();

        Map<String, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        // 모든 선택 항목이 유효한지 확인 (존재/판매상태/재고 등 정책에 맞게)
        for (CartItem cartItem : selectedCartItems) {
            Item item = itemMap.get(cartItem.getItemId());
            if (item == null) {
                throw new RuntimeException("상품을 찾을 수 없습니다: itemId=" + cartItem.getItemId());
            }
            // 선택: 임시 주문이라도 기본 검증
            if (item.getStatus() != ItemStatus.SALE) {
                throw new RuntimeException("판매 중이 아닌 상품입니다: itemId=" + cartItem.getItemId());
            }
            if (item.getStockQuantity() < cartItem.getCount()) {
                throw new RuntimeException("재고가 부족합니다: itemId=" + cartItem.getItemId()
                        + ", 요청수량=" + cartItem.getCount() + ", 재고=" + item.getStockQuantity());
            }
        }

        // 3) CartItem → OrderItem 변환 (카트 가격을 주문 가격으로 스냅샷)
        List<OrderItem> orderItems = selectedCartItems.stream()
                .map(this::convertCartItemToOrderItem)
                .toList();

        // 4) 금액 계산 (itemMap 재사용)
        long totalItemAmount = orderItems.stream()
                .mapToLong(oi -> oi.getOrderPrice() * oi.getCount())
                .sum();

        long totalDeliveryFee = getTotalDeliveryFee(orderItems, itemMap);

        long totalAmount = totalItemAmount + totalDeliveryFee;

        // 5) 임시 Order 생성 및 연관관계 설정
        Order order = new Order();
        order.setBuyer(buyer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.TEMPORARY);
        order.setDeliveryFee(totalDeliveryFee);
        // 필요 시 임시 주문명 부여
        order.setOrderName("임시주문 " + selectedCartItems.size() + "건");
        order.setTotalAmount(totalAmount);
        order.setOriginalAmount(totalAmount);

        orderItems.forEach(order::addOrderItem);

        // 6) 저장
        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }

    /**
     * 배송비 계산 로직 (판매자 중복 제거)
     */
    private static long getTotalDeliveryFee(List<OrderItem> orderItems, Map<String, Item> itemMap) {
        Map<String, Long> sellerDeliveryFeeMap = new HashMap<>();
        for (OrderItem oi : orderItems) {
            Item item = itemMap.get(oi.getItemId());
            String sellerId = item.getSellerId();
            Long deliveryFee = item.getDeliveryFee() != null ? item.getDeliveryFee() : 0L;
            sellerDeliveryFeeMap.putIfAbsent(sellerId, deliveryFee);
        }
        return sellerDeliveryFeeMap.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * 상품페이지에서 바로 구매
     */
    public Long createTemporaryOrder(Long buyerId, String itemId, Long count) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        if (count <= 0) {
            throw new RuntimeException("수량은 1 이상이어야 합니다.");
        }

        if (item.getStockQuantity() < count) {
            throw new RuntimeException("재고가 부족합니다.");
        }

        // 임시 주문 생성
        Order order = new Order();
        order.setBuyer(buyer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.TEMPORARY); // 또는 임시 상태가 있으면 사용

        // 주문 아이템 생성
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setCount(count);
        orderItem.setOrderPrice(item.getPrice());
        order.addOrderItem(orderItem);

        // 1. 상품 총액 계산
        Long totalItemAmount = item.getPrice() * count;

        // 2. 판매자별 배송비 계산 (지금은 상품 1개이지만 구조는 동일)
        Long deliveryFee = getDeliveryFee(item);

        // 3. 총 결제금액 = 상품금액 + 판매자 배송비
        Long totalAmount = totalItemAmount + deliveryFee;

        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setOriginalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        return savedOrder.getId();
    }

    /**
     * 선택된 상품 주문 완료 처리
     */

    public void completeSelectedOrder(Long buyerId, Long orderId, OrderRequestDto orderRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다"));

        // 1. 재고 확인
        validateStockFromOrderItems(order.getOrderItems());

        // 2. 배송지 주소 조회 및 권한 체크, 배송 정보 설정
        for (OrderItem orderItem : order.getOrderItems()) {
            Delivery delivery = createDeliveryWithAddressId(buyerId, orderRequest.getAddressId());
            delivery.setOrderItem(orderItem);
            orderItem.setDelivery(delivery);
        }

        String couponCode = orderRequest.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            // 상품 총액 계산을 위해 orderItems를 넘겨 내부에서 상품총액 산출
            computeCouponDiscount(order.getOrderItems(), couponCode);
            Coupon coupon = couponService.getCouponByCode(couponCode)
                    .orElseThrow(() -> new RuntimeException("쿠폰이 조회되지 않습니다."));
            order.setCoupon(coupon);
        }

        // 3. 결제 정보 설정 (총금액, 주문명, pgOrderId 등)
        setPaymentInfoFromOrderItems(order, order.getOrderItems());

        // 4. 재고 차감
        reduceStockFromOrderItems(order.getOrderItems());

        // 5. 주문 상태 주문 완료로 변경
        order.setStatus(OrderStatus.PAID);

        // 6. 포인트 적립 (최종 결제금액 기준)
        // 만료 1년
        ledgerPointService.savePoints(buyerId,
                Math.max(1L, Math.round(order.getTotalAmount() * 0.02)),
                LocalDateTime.now().plusYears(1),
                order.getId(),
                "결제 포인트 적립",
                "system");

        // 7. 장바구니가 존재하면 주문된 아이템 제거
        Optional<Cart> optionalCart = cartRepository.findByBuyerId(buyerId);
        if (optionalCart.isPresent()) {
            Cart cart = optionalCart.get();
            List<String> orderedItemIds = order.getOrderItems().stream()
                    .map(OrderItem::getItemId)
                    .distinct()
                    .toList();
            cart.getCartItems().removeIf(cartItem -> orderedItemIds.contains(cartItem.getItemId()));
            cartRepository.save(cart);
        } else {
            log.info("주문자 {}의 장바구니가 없어 아이템 제거를 건너뜁니다.", buyerId);
        }

        redisUtil.deleteData("order:" + orderId + ":couponAmount");
        redisUtil.deleteData("order:" + orderId + ":pointAmount");

        // 8. 최종 저장
        orderRepository.save(order);

    }

    private void computeCouponDiscount(List<OrderItem> orderItems, String couponCode) {
        long totalItemAmount = orderItems.stream()
                .mapToLong(oi -> oi.getOrderPrice() * oi.getCount())
                .sum();
        // 검증 및 사용 처리 -> 할인 금액 반환
        couponService.applyCouponIfValid(couponCode, totalItemAmount);
    }

    /**
     * OrderItem 기반 재고 확인
     */
    private void validateStockFromOrderItems(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            Item item = itemRepository.findById(orderItem.getItemId())
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + orderItem.getItemId()));
            if (item.getStockQuantity() < orderItem.getCount()) {
                throw new RuntimeException("재고가 부족합니다. 상품: " + item.getName() +
                        ", 요청수량: " + orderItem.getCount() +
                        ", 현재재고: " + item.getStockQuantity());
            }
        }
    }

    /**
     * OrderItem 기반 재고 차감
     */
    private void reduceStockFromOrderItems(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            Item item = itemRepository.findById(orderItem.getItemId())
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + orderItem.getItemId()));
            item.removeStock(orderItem.getCount());
            itemRepository.save(item);
        }
    }

    /**
     * OrderItem기반 주문 정보 설정
     */
    private void setPaymentInfoFromOrderItems(Order order, List<OrderItem> orderItems) {

        // 1. 주문명 생성
        String orderName = generateOrderNameFromOrderItems(orderItems);

        // 2. PG 주문번호 생성
        String pgOrderId = "ORDER_" + order.getId() + "_" + System.currentTimeMillis();

        // 3. 주문 엔티티에 값 설정
        order.setPgOrderId(pgOrderId);
        order.setOrderName(orderName);
    }

    /**
     * OrderItem 기반 주문명 생성 메서드
     */
    private String generateOrderNameFromOrderItems(List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            return "상품";
        }

        // 첫 번째 상품 정보 조회
        OrderItem firstItem = orderItems.getFirst();
        Item item = itemRepository.findById(firstItem.getItemId())
                .orElse(null);
        String firstItemName = (item != null) ? item.getName() : "상품";

        long itemCount = orderItems.size();
        if (itemCount == 1) {
            return firstItemName;
        } else {
            return firstItemName + " 외 " + (itemCount - 1) + "건";
        }
    }

    /**
     * 주소 ID로 Delivery 생성
     */
    private Delivery createDeliveryWithAddressId(Long buyerId, Long addressId) {
        // 주소 조회 및 권한 확인
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("주소를 찾을 수 없습니다: " + addressId));

        // 주소 소유권 확인 (해당 구매자의 주소인지 검증)
        if (address.getBuyer() != null && !address.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("해당 주소를 사용할 권한이 없습니다");
        }

        Delivery delivery = new Delivery();
        delivery.setAddress(address);
        delivery.setStatus(DeliveryStatus.READY);
        return delivery;
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));
    }

    /**
     * 주문 ID로 구매자 ID 조회
     */
    @Transactional(readOnly = true)
    public Long getBuyerIdByOrderId(Long orderId) {
        log.debug("주문 ID로 구매자 ID 조회 시작 - orderId: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없습니다 - orderId: {}", orderId);
                    return new RuntimeException("주문을 찾을 수 없습니다: " + orderId);
                });

        if (order.getBuyer() == null) {
            log.error("주문에 구매자 정보가 없습니다 - orderId: {}", orderId);
            throw new RuntimeException("주문에 구매자 정보가 없습니다: " + orderId);
        }

        Long buyerId = order.getBuyer().getId();
        log.debug("구매자 ID 조회 완료 - orderId: {}, buyerId: {}", orderId, buyerId);

        return buyerId;
    }

    /**
     * 구매자의 주문 내역을 최신순으로 조회하여 DTO 리스트로 반환
     */
    public List<OrderHistoryDto> getOrderHistory(Long buyerId) {
        // 1. 주문 목록 조회
        List<Order> orders = orderRepository.findByBuyer_IdOrderByOrderDateDesc(buyerId);

        // 2. 반환할 DTO 리스트 준비
        List<OrderHistoryDto> orderHistoryDtos = new ArrayList<>();

        for (Order order : orders) {
            // 2-1. 해당 주문의 아이템 목록 조회 및 DTO 변환
            List<OrderHistoryItemDto> itemDtos = new ArrayList<>();

            for (OrderItem orderItem : order.getOrderItems()) {
                try {
                    // MongoDB에서 Item 정보 조회
                    Item item = itemService.getItemById(orderItem.getItemId());

                    // DTO 변환 후 리스트에 추가
                    OrderHistoryItemDto itemDto = OrderHistoryItemDto.from(orderItem, item);
                    itemDtos.add(itemDto);
                } catch (Exception e) {
                    log.error("아이템 조회 중 오류 발생: orderItemId={}, itemId={}",
                            orderItem.getId(), orderItem.getItemId(), e); // 혹은 로그 찍기
                    // 필요하면 기본값 세팅 or 예외 무시
                }
            }

            // 2-2. 주문 단위 DTO 생성 후 최종 리스트에 추가
            OrderHistoryDto orderDto = new OrderHistoryDto(order, itemDtos);
            orderHistoryDtos.add(orderDto);
        }

        return orderHistoryDtos;
    }

    /**
     * 주문 내역 조회 메서드
     */
    public OrderHistoryDto getLatestOrderByBuyerId(Long buyerId) {

        Order order = orderRepository.findTop1ByBuyer_IdAndStatusOrderByOrderDateDesc(buyerId, OrderStatus.PAID);

        if (order == null) {  // 주문 없으면 null임
            return null;       // null 반환해서 컨트롤러에서 204 No Content 처리 가능
        }

        // 주문 있으면 DTO 변환 진행
        List<OrderHistoryItemDto> itemDtos = order.getOrderItems().stream()
                .map(orderItem -> {
                    Item item = itemService.getItemById(orderItem.getItemId());
                    return OrderHistoryItemDto.from(orderItem, item);
                }).toList();

        return new OrderHistoryDto(order, itemDtos);
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getRecentMonthOrders(Long buyerId) {

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusMonths(1);

        // 1) 한달 내 주문 헤더 Top-N (예: 100건 상한)
        List<Order> orders = orderRepository.findRecentPaidOrdersByBuyer(buyerId, from, to, 100);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 주문아이디 목록으로 OrderItem 일괄 로딩 (배치 조회)
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        // OrderItem을 주문별로 묶기 위한 맵
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findAllByOrder_IdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        // 3) DTO 변환: Mongo Item은 서비스 통해 조회
        return orders.stream()
                .map(o -> {
                    List<OrderItem> orderItems = itemsByOrderId.getOrDefault(o.getId(), List.of());
                    List<OrderHistoryItemDto> itemDtos = orderItems.stream()
                            .map(oi -> {
                                Item item = itemService.getItemById(oi.getItemId());
                                return OrderHistoryItemDto.from(oi, item);
                            })
                            .toList();
                    return new OrderHistoryDto(o, itemDtos);
                })
                .toList();
    }

    /**
     * 로그인한 구매자의 주문 내역 단건 조회
     */
    public OrderHistoryDto getOrderDetail(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdAndBuyerIdWithItems(orderId, buyerId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // 주문 있으면 DTO 변환 진행
        List<OrderHistoryItemDto> itemDtos = order.getOrderItems().stream()
                .map(orderItem -> {
                    Item item = itemService.getItemById(orderItem.getItemId());
                    return OrderHistoryItemDto.from(orderItem, item);
                }).toList();

        return new OrderHistoryDto(order, itemDtos);
    }

    /**
     * 주문 완료 페이지 정보
     */
    @Transactional(readOnly = true)
    public OrderCompleteInfoDto getOrderCompleteInfo(Long orderId, Long buyerId) {
        Order order = getOrderById(orderId);
        order.validateOwner(buyerId);   //주문 권한 체크

        Coupon coupon = order.getCoupon();

        Payment payment = paymentRepository.findByOrder(order).orElse(null);

        // 상품 DTO 변환
        List<OrderCompleteInfoDto.OrderCompleteItemDto> itemDtos = order.getOrderItems().stream()
                .map(oi -> OrderCompleteInfoDto.OrderCompleteItemDto.from(oi, itemService.getItemById(oi.getItemId())))
                .toList();

        // 배송 DTO 변환
        List<OrderCompleteInfoDto.OrderCompleteDeliveryDto> deliveryDtos = order.getOrderItems().stream()
                .map(OrderCompleteInfoDto.OrderCompleteDeliveryDto::from)
                .toList();

        // 적립 포인트 Ledger 조회
        PointLedger savedLedger = pointLedgerRepository.findTopByBuyer_IdAndTypeAndRefOrderIdOrderByOccurredAtDesc(
                buyerId, PointTransactionType.SAVE, order.getId()
        ).orElse(null);

        // 최종 DTO 생성
        return OrderCompleteInfoDto.from(order, itemDtos, deliveryDtos, coupon, payment, savedLedger);
    }

    /**
     * 현재 사용자의 모든 임시 주문 삭제 (TEMPORARY 상태인 주문들을 일괄 삭제)
     */
    public long deleteAllTemporaryOrders(Long buyerId) {
        // 해당 구매자의 TEMPORARY 상태인 모든 주문 조회
        List<Order> temporaryOrders = orderRepository.findByBuyerIdAndStatus(buyerId, OrderStatus.TEMPORARY);

        for (Order temporaryOrder : temporaryOrders) {
            redisUtil.deleteData("order:" + temporaryOrder.getId() + ":couponAmount");
            redisUtil.deleteData("order:" + temporaryOrder.getId() + ":pointAmount");
        }

        if (temporaryOrders.isEmpty()) {
            log.info("삭제할 임시 주문이 없습니다. buyerId: {}", buyerId);
            return 0;
        }

        // 모든 임시 주문 삭제
        orderRepository.deleteAll(temporaryOrders);

        long deletedCount = temporaryOrders.size();
        log.info("임시 주문 {}건이 삭제되었습니다. buyerId: {}", deletedCount, buyerId);

        return deletedCount;
    }

    /**
     * 환불 가능한 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getRefundableOrderItems(Long buyerId) {
        // 결제 성공(SUCCESS), 배송 준비/배송 중 등 환불 가능한 주문만 조회(상황에 따라 필터링 확장 가능)
        List<Order> orders = orderRepository.findByBuyerIdAndStatusInAndPaymentStatus(
                buyerId,
                List.of(OrderStatus.ORDERED, OrderStatus.PAID),
                "SUCCESS"
        );

        List<OrderHistoryDto> result = new ArrayList<>();

        for (Order order : orders) {
            // 환불 요청 또는 완료가 아닌 아이템만 필터링
            List<OrderItem> refundableItems = order.getOrderItems().stream()
                    .filter(oi -> oi.getRefundStatus() == RefundStatus.NONE)
                    .toList();

            if (!refundableItems.isEmpty()) {
                List<OrderHistoryItemDto> itemDtos = new ArrayList<>();
                for (OrderItem oi : refundableItems) {
                    try {
                        Item item = itemService.getItemById(oi.getItemId());
                        itemDtos.add(OrderHistoryItemDto.from(oi, item));
                    } catch (Exception e) {
                        log.error("환불 가능 내역 중 아이템 조회 오류 : itemId={}", oi.getItemId(), e);
                    }
                }
                result.add(new OrderHistoryDto(order, itemDtos));
            }
        }
        return result;
    }

    /**
     * 아이템 단위 환불 요청
     */
    public void requestRefund(Long orderId, Long orderItemId, Long buyerId, String reason, String detail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("해당 주문에 대한 권한이 없습니다.");
        }

        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new EntityNotFoundException("해당 buyer가 없습니다."));

        Optional<OrderItem> optionalOrderItem = order.getOrderItems()
                .stream()
                .filter(oi -> oi.getId().equals(orderItemId))
                .findFirst();

        if (optionalOrderItem.isEmpty()) {
            throw new RuntimeException("해당 주문 아이템을 찾을 수 없습니다.");
        }

        OrderItem orderItem = optionalOrderItem.get();

        if (orderItem.getRefundStatus() != RefundStatus.NONE) {
            throw new RuntimeException("이미 환불 요청 또는 완료된 아이템입니다.");
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderItem(orderItem);
        refundRequest.setBuyer(buyer);
        refundRequest.setRefundReason(reason);
        refundRequest.setRefundDetail(detail);
        refundRequest.setRequestedAt(LocalDateTime.now());
        orderItem.setRefundRequest(refundRequest);

        // 환불 요청 상태로 변경
        orderItem.setRefundStatus(RefundStatus.REQUESTED);

        orderRepository.save(order);
    }

    /**
     * 요청한 환불 목록 보기
     */
    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getRefundRequestedItems(Long buyerId) {
        // 해당 buyer의 모든 주문을 가져온다
        List<Order> orders = orderRepository.findByBuyer_IdOrderByOrderDateDesc(buyerId);

        List<OrderHistoryDto> refundRequestedOrders = new ArrayList<>();

        for (Order order : orders) {
            // 주문 아이템 중 환불 요청 또는 완료 상태인 아이템 필터링
            List<OrderItem> refundItems = order.getOrderItems().stream()
                    .filter(oi -> oi.getRefundStatus() == RefundStatus.REQUESTED || oi.getRefundStatus() == RefundStatus.REFUNDED)
                    .toList();

            if (!refundItems.isEmpty()) {
                // 각 아이템에 대해 Item 정보 조회 및 DTO 변환
                List<OrderHistoryItemDto> itemDtos = new ArrayList<>();
                for (OrderItem oi : refundItems) {
                    try {
                        Item item = itemService.getItemById(oi.getItemId());
                        OrderHistoryItemDto dto = OrderHistoryItemDto.from(oi, item);
                        itemDtos.add(dto);
                    } catch (Exception e) {
                        // 예외 로깅 및 무시 또는 기본값 처리
                        log.error("환불 내역 아이템 조회 중 오류: itemId={}", oi.getItemId(), e);
                    }
                }
                refundRequestedOrders.add(new OrderHistoryDto(order, itemDtos));
            }
        }

        refundRequestedOrders.sort(Comparator.comparing(
                dto -> dto.getItems().stream()
                        .map(OrderHistoryItemDto::getRequestedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.MIN),
                Comparator.reverseOrder()
        ));

        return refundRequestedOrders;
    }

    /**
     * 판매자 환불 요청 및 완료 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RefundRequestDetailDto> getRefundRequestsByStatus(Long sellerId, RefundStatus refundStatus) {
        // 1. 판매자가 등록한 상품 id 목록 조회
        List<Item> sellerItems = itemService.getItemsBySellerId(String.valueOf(sellerId));
        Set<String> sellerItemIds = sellerItems.stream()
                .map(Item::getId)
                .collect(Collectors.toSet());

        // 2. 모든 주문 조회 (실제 운영 환경에선 조건절로 최적화 권장)
        List<Order> allOrders = orderRepository.findAll();

        // 3. 결과 담을 리스트 초기화
        List<RefundRequestDetailDto> result = new ArrayList<>();

        // 4. 각 주문별로 주문자(buyer), 주문 아이템 순회
        for (Order order : allOrders) {
            Buyer buyer = order.getBuyer();

            for (OrderItem oi : order.getOrderItems()) {
                // 주문아이템에 매핑된 배송 정보와 주소 획득
                Delivery delivery = oi.getDelivery();
                Address address = (delivery != null) ? delivery.getAddress() : null;

                // (환불상태 parameter와 판매자 상품만 필터)
                if (oi.getRefundStatus() == refundStatus && sellerItemIds.contains(oi.getItemId())) {
                    // 6. 아이템 정보 조회
                    Item item = itemService.getItemById(oi.getItemId());
                    // 7. refundRequest 정보 가져오기
                    RefundRequest refundRequest = oi.getRefundRequest();

                    // 8. 내부 DTO 객체 생성
                    BuyerInfo buyerInfo = new BuyerInfo(buyer);
                    AddressInfo addressInfo = (address != null) ? new AddressInfo(address) : null;
                    RefundInfo refundInfo = new RefundInfo(refundRequest);

                    // 9. DTO 변환 후 결과에 추가
                    result.add(new RefundRequestDetailDto(order, oi, item, buyerInfo, addressInfo, refundInfo));

                    // 10. requestedAt 기준으로 내림차순 정렬
                    result.sort(Comparator.comparing(dto -> dto.getRefundInfo().getRequestedAt(), Comparator.nullsLast(Comparator.reverseOrder())));
                }
            }
        }
        return result;
    }

    /**
     * 판매자 환불 요청 완료
     */
    public String completeRefund(Long orderItemId, Long sellerId) {
        // 주문 아이템 조회
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("주문 아이템을 찾을 수 없습니다: " + orderItemId));

        // 판매자 권한 검증: 해당 주문 아이템의 상품이 sellerId가 맞는지 확인
        Item item = itemService.getItemById(orderItem.getItemId());
        if (!item.getSellerId().equals(String.valueOf(sellerId))) {
            throw new RuntimeException("해당 판매자가 주문 아이템의 판매자가 아닙니다.");
        }

        // 환불 상태가 요청된 상태인지 확인
        if (orderItem.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new RuntimeException("현재 환불 요청 상태가 아닙니다.");
        }

        // 환불 상태 변경: REFUNDED
        orderItem.setRefundStatus(RefundStatus.REFUNDED);
        orderItemRepository.save(orderItem);

        // 재고 복원 (필요한 경우)
        item.addStock(orderItem.getCount());
        itemRepository.save(item);

        // (필요시) 주문 상태 또는 결제 상태 업데이트 등 추가 처리 가능
        long price = orderItem.getOrderPrice() * orderItem.getCount();
        long usePointAmount = -(orderItem.getOrder().getPointLedger().getAmount());
        if (price >= usePointAmount) {
            Long buyerId = orderItem.getOrder().getBuyer().getId();
            Long pointLedgerId = orderItem.getOrder().getPointLedger().getId();
            ledgerPointService.cancelUse(buyerId, pointLedgerId, "환불에 의한 포인트 사용 취소", "System");
        } else {
            return "사용 포인트 보다 작은 금액의 상품을 환불 할 경우 포인트는 반환되지 않습니다.";
        }
        return "환불이 완료 되었습니다.";
    }

    /**
     * 판매자의 item의 주문 기록 조회
     */
    public List<SellerOrderDto> getSellerOrders(Long sellerId) {
        // 1. MongoDB에서 해당 판매자의 상품 목록을 모두 조회 (sellerId는 String)
        List<Item> sellerItems = itemRepository.findBySellerId(sellerId.toString());

        // 2. 각 상품의 ID만 추출해서 Set으로 변환 (중복 제거 및 빠른 조회를 위해)
        Set<String> sellerItemIds = sellerItems.stream()
                .map(Item::getId)
                .collect(Collectors.toSet());

        // 3. 판매자의 상품이 하나도 없다면 빈 리스트 반환
        if (sellerItemIds.isEmpty()) return Collections.emptyList();

        // 4. OrderItem 테이블에서 itemId가 판매자의 상품인 주문 아이템들만 조회 (JPA에서 IN 쿼리 생성)
        List<OrderItem> orderItems = orderItemRepository.findAllByItemIdInOrderByOrder_OrderDateDesc(new ArrayList<>(sellerItemIds));

        // 5. itemId로 빠르게 Item 객체를 참조할 수 있도록 Map으로 구성
        Map<String, Item> itemMap = sellerItems.stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        return orderItems.stream()
                // 6. 필터링된 주문 아이템들을 DTO 형태로 변환하여 리스트에 담음
                // ① 주문 상태가 TEMPORARY가 아닌 경우만 필터링
                .filter(orderItem -> orderItem.getOrder().getStatus() != OrderStatus.TEMPORARY)
                .map(orderItem -> new SellerOrderDto(
                        orderItem.getOrder(),               // Order 엔티티
                        orderItem,                          // OrderItem 엔티티
                        itemMap.get(orderItem.getItemId())  // Item 엔티티
                ))
                .collect(Collectors.toList());              // 7. 최종 주문 목록 반환
    }

    /**
     * 판매자의 item의 주문 상세 조회
     */
    public SellerOrderDetailDto getSellerOrderDetail(Long sellerId, Long orderItemId) {
        // 주문 아이템 조회
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 항목입니다."));

        // 상품 조회
        Item item = itemRepository.findById(orderItem.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다."));

        // 판매자 검증
        if (!item.getSellerId().equals(sellerId.toString())) {
            throw new AccessDeniedException("해당 주문에 접근할 수 없습니다.");
        }

        // 주문, 구매자, 배송 조회
        Order order = orderItem.getOrder();
        Buyer buyer = order.getBuyer();

        Delivery delivery = orderItem.getDelivery(); // OrderItem에서 Delivery 가져오기

        return new SellerOrderDetailDto(order, orderItem, item, buyer, delivery);
    }


    /**
     * 판매자가 임시로 배송 완료 처리
     */
    public void completeDelivery(Long orderItemId, Long sellerId) {
        // 주문 아이템 조회
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("주문 아이템을 찾을 수 없습니다: " + orderItemId));

        // 판매자 권한 검증: 주문 아이템 상품의 판매자가 맞는지 확인
        Item item = itemService.getItemById(orderItem.getItemId());
        if (!item.getSellerId().equals(String.valueOf(sellerId))) {
            throw new RuntimeException("해당 판매자가 주문 아이템의 판매자가 아닙니다.");
        }

        // 배송 정보 존재 확인
        Delivery delivery = orderItem.getDelivery();
        if (delivery == null) {
            throw new RuntimeException("배송 정보가 존재하지 않습니다.");
        }

        // 이미 배송 완료 상태인지 확인
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new RuntimeException("이미 배송 완료된 주문입니다.");
        }

        // 배송 상태 변경
        delivery.setStatus(DeliveryStatus.DELIVERED);

        // 필요시 주문 상태도 변경 가능 (예: 배송 완료 후 결제 완료 상태 유지 또는 별도 상태 변경)

        // 저장
        orderItemRepository.save(orderItem);
        // Delivery는 cascade 옵션에 따라 자동으로 저장됨
    }

    /**
     * 카트아이템에서 오더아이템으로 변환
     */
    private OrderItem convertCartItemToOrderItem(CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(cartItem.getItemId());
        orderItem.setOrderPrice(cartItem.getCartPrice());  // 장바구니 가격을 주문 가격으로
        orderItem.setCount(cartItem.getCount());
        return orderItem;
    }

    /**
     * 아이템에서 배송비 가져오는 메서드
     */
    private static Long getDeliveryFee(Item item) {
        Long deliveryFee = 0L;
        try {
            deliveryFee = item.getDeliveryFee();
        } catch (NumberFormatException e) {
            log.info("{}숫자 변환 실패 시 배송비 0 처리", deliveryFee); // 숫자 변환 실패 시 배송비 0 처리
        }
        return deliveryFee;
    }

    public String applyPoints(Long buyerId, Long orderId, Long usePointAmount) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        long activePoints = ledgerPointService.getActive(buyerId);
        if (usePointAmount > activePoints) {
            throw new RuntimeException("사용가능 포인트 부족");
        }

        String redisKey = "order:" + orderId + ":pointAmount";

        if (redisUtil.getData(redisKey) != null) {
            String data = redisUtil.getData(redisKey);
            order.setTotalAmount(order.getTotalAmount() + Long.parseLong(data));
        }

        order.setTotalAmount(order.getTotalAmount() - usePointAmount);
        redisUtil.setData(redisKey, String.valueOf(usePointAmount));
//        order.setUsedPointAmount(usePointAmount);

        orderRepository.save(order);

        return "포인트 " + usePointAmount + "원이 적용되었습니다.";
    }
}
