package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.*;
import com.shop.respawn.dto.review.CountReviewDto;
import com.shop.respawn.dto.review.ReviewWithItemDto;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.OrderItemRepository;
import com.shop.respawn.repository.mongo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.shop.respawn.util.MaskingUtil.maskMiddleFourChars;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;   // MongoDB 리뷰 저장소
    private final BuyerRepository buyerRepository;     // RDBMS 구매자
    private final OrderItemRepository orderItemRepository; // RDBMS 주문 아이템
    private final ItemService itemService;

    /**
     * 리뷰 작성
     * 배송 완료된 주문 아이템에 대해서만 MongoDB에 리뷰 저장
     */
    public void createReview(Long buyerId, String orderItemId, int rating, String content) {

        // 주문 아이템 조회 (RDBMS)
        OrderItem orderItem = orderItemRepository.findById(Long.valueOf(orderItemId))
                .orElseThrow(() -> new RuntimeException("주문 아이템을 찾을 수 없습니다."));

        // 주문 및 구매자 검증
        Order order = orderItem.getOrder();
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("해당 주문 아이템에 대한 권한이 없습니다.");
        }

        // 배송 완료 여부 확인 - Order가 아닌 OrderItem의 Delivery에서 상태 확인
        Delivery delivery = orderItem.getDelivery();
        if (delivery == null || delivery.getStatus() != DeliveryStatus.DELIVERED) {
            throw new RuntimeException("배송이 완료된 주문 아이템에 대해서만 리뷰를 작성할 수 있습니다.");
        }

        // 중복 리뷰 확인 (MongoDB)
        if (reviewRepository.findByOrderItemId(orderItemId).isPresent()) {
            throw new RuntimeException("이미 리뷰를 작성한 주문 아이템입니다.");
        }

        // 구매자 존재 확인
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다."));

        String itemId = orderItem.getItemId();

        // 리뷰 생성 및 저장 (MongoDB)
        Review review = Review.builder()
                .buyerId(String.valueOf(buyer.getId()))
                .orderItemId(orderItemId)
                .itemId(itemId)
                .rating(rating)
                .content(content)
                .createdDate(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
    }

    /**
     * 판매자 ID로 판매한 아이템들의 리뷰 리스트 조회
     */
    @Transactional(readOnly = true)
    public Page<ReviewWithItemDto> getReviewsBySellerId(String sellerId, Pageable pageable) {
        // 1) 판매자의 아이템 ID 목록
        List<Item> sellerItems = itemService.getItemsBySellerId(sellerId);
        if (sellerItems.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> sellerItemIds = sellerItems.stream().map(Item::getId).toList();

        // 2) MongoDB: 아이템 ID IN + createdDate DESC 페이징
        Page<Review> reviewPage = reviewRepository.findByItemIdIn(sellerItemIds, pageable);
        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3) 연관 키 수집
        List<String> orderItemIdStrs = reviewPage.getContent().stream()
                .map(Review::getOrderItemId).toList();

        // 4) 주문아이템 일괄 조회 (RDBMS)
        //    이미 존재하는 커스텀 메서드/쿼리를 활용: findAllByIdIn 또는 findAllById(List<Long>) 구현 필요
        List<Long> orderItemIds = orderItemIdStrs.stream().map(Long::valueOf).toList();
        List<OrderItem> orderItems = orderItemRepository.findAllById(orderItemIds);

        // 5) 매핑 테이블 구성
        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, oi -> oi));

        // 6) 아이템 매핑 (이미 sellerItems를 보유하므로 검색 비용 O(1) 위해 map 구성)
        Map<String, Item> itemMap = sellerItems.stream()
                .collect(Collectors.toMap(Item::getId, it -> it));

        // 7) 구매자 마스킹을 위해 구매자 username 조회 N+1 방지
        // buyerId 리스트를 모아 일괄 조회하도록 BuyerRepository에 findAllById(Collection<Long>) 사용
        return getReviewWithItemDtos(pageable, reviewPage, orderItemMap, itemMap);
    }

    /**
     * 판매자 ID와 특정 아이템 ID로 필터링된 리뷰 리스트 조회
     */
    @Transactional(readOnly = true)
    public Page<ReviewWithItemDto> getReviewsBySellerIdAndItemId(String sellerId, String itemId, Pageable pageable) {
        // 1) 소유 검증
        List<Item> sellerItems = itemService.getItemsBySellerId(sellerId);
        boolean ownsItem = sellerItems.stream().anyMatch(i -> i.getId().equals(itemId));
        if (!ownsItem) {
            return Page.empty(pageable);
        }

        // 2) MongoDB: 특정 itemId 페이징
        Page<Review> reviewPage = reviewRepository.findByItemId(itemId, pageable);
        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3) orderItemId 일괄 조회
        List<Long> orderItemIds = reviewPage.getContent().stream()
                .map(Review::getOrderItemId).map(Long::valueOf).toList();
        List<OrderItem> orderItems = orderItemRepository.findAllById(orderItemIds);
        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, oi -> oi));

        // 4) 단일 아이템 조회 및 매핑
        Item item = sellerItems.stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseGet(() -> itemService.getItemById(itemId));
        Map<String, Item> itemMap = Map.of(itemId, item);

        return getReviewWithItemDtos(pageable, reviewPage, orderItemMap, itemMap);
    }

    private PageImpl<ReviewWithItemDto> getReviewWithItemDtos(Pageable pageable, Page<Review> reviewPage, Map<Long, OrderItem> orderItemMap, Map<String, Item> itemMap) {
        // 구매자명 일괄 조회
        List<Long> buyerIds = reviewPage.getContent().stream()
                .map(r -> Long.parseLong(r.getBuyerId()))
                .distinct().toList();
        Map<Long, String> buyerUsernameMap = buyerRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(Buyer::getId, Buyer::getUsername));

        // DTO 변환
        List<ReviewWithItemDto> dtos = reviewPage.getContent().stream().map(r -> {
            OrderItem oi = orderItemMap.get(safeLong(r.getOrderItemId()));
            Item item = null;
            if (oi != null) {
                item = itemMap.get(oi.getItemId());
            }
            if (item == null) {
                item = itemMap.get(r.getItemId());
            }

            Long bid = safeLong(r.getBuyerId());
            String masked = "알 수 없는 사용자";
            if (bid != null) {
                String username = buyerUsernameMap.get(bid);
                if (username != null) masked = maskMiddleFourChars(username);
            }

            Order order = (oi != null) ? oi.getOrder() : null;
            return new ReviewWithItemDto(r, item, masked, order); // 여기서 itemId, itemName, imageUrl, price 포함됨
        }).toList();

        return new PageImpl<>(dtos, pageable, reviewPage.getTotalElements());
    }

    private Long safeLong(String s) {
        try { return Long.valueOf(s); } catch (Exception e) { return null; }
    }

    // 특정 아이템(itemId)에 대한 모든 리뷰 가져오기
    @Transactional(readOnly = true)
    public Page<ReviewWithItemDto> getReviewsByItemId(String itemId, Pageable pageable) {
        // 1) Mongo: 특정 아이템 리뷰 페이징
        Page<Review> reviewPage = reviewRepository.findByItemId(itemId, pageable); // 메서드명 단순화 권장
        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2) 연관 키 수집
        List<String> orderItemIdStrs = reviewPage.getContent().stream()
                .map(Review::getOrderItemId).toList();
        List<Long> orderItemIds = orderItemIdStrs.stream().map(s -> {
            try { return Long.valueOf(s); } catch (Exception e) { return null; }
        }).filter(Objects::nonNull).toList();

        // 3) 주문아이템 일괄 조회 (RDB)
        List<OrderItem> orderItems = orderItemRepository.findAllById(orderItemIds);
        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, oi -> oi));

        // 4) 아이템 정보 확보 (이미 itemId 단건이므로 1회 조회 또는 캐시 활용)
        //    성능을 위해 Mongo에서 필요한 필드만 가져오는 partial 조회 사용 가능
        Item item = itemService.getItemById(itemId); // 또는 partial 메서드 활용
        Map<String, Item> itemMap = Map.of(itemId, item);

        // 5) 구매자 이름 일괄 조회 후 마스킹
        List<Long> buyerIds = reviewPage.getContent().stream()
                .map(r -> {
                    try { return Long.parseLong(r.getBuyerId()); } catch (Exception e) { return null; }
                }).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> buyerUsernameMap = buyerRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(Buyer::getId, Buyer::getUsername));

        // 6) DTO 변환 (itemId, itemName, imageUrl, price 포함)
        List<ReviewWithItemDto> content = reviewPage.getContent().stream().map(r -> {
            OrderItem oi = null;
            try { oi = orderItemMap.get(Long.valueOf(r.getOrderItemId())); } catch (Exception ignore) {}
            Item it = itemMap.getOrDefault(r.getItemId(), item);

            Long bid = null;
            try { bid = Long.parseLong(r.getBuyerId()); } catch (Exception ignore) {}
            String masked = "알 수 없는 사용자";
            if (bid != null) {
                String username = buyerUsernameMap.get(bid);
                if (username != null) masked = maskMiddleFourChars(username);
            }

            Order order = (oi != null) ? oi.getOrder() : null;
            return new ReviewWithItemDto(r, it, masked, order);
        }).toList();

        return new PageImpl<>(content, pageable, reviewPage.getTotalElements());
    }

    public boolean existsReviewByOrderItemId(Long buyerId, String orderItemId) {
        // 본인 리뷰만 체크(옵션), buyerId 체크 생략하면 공용 체크
        return reviewRepository.findByOrderItemId(orderItemId)
                .filter(review -> review.getBuyerId().equals(String.valueOf(buyerId)))
                .isPresent();
    }

    public List<ReviewWithItemDto> convertReviewsToDtos(List<Review> reviews, List<Item> relatedItems) {
        return reviews.stream()
                .map(review -> {
                    // 리뷰 작성자 ID
                    String buyerId = review.getBuyerId();
                    String maskedUsername;

                    try {
                        String buyerUsername = buyerRepository.findById(Long.parseLong(buyerId))
                                .map(Buyer::getUsername)
                                .orElse("알 수 없는 사용자");
                        maskedUsername = maskMiddleFourChars(buyerUsername);
                    } catch (NumberFormatException e) {
                        maskedUsername = "알 수 없는 사용자";
                    }

                    // 리뷰의 OrderItemId로부터 itemId 확인 (OrderItem에서)
                    String orderItemId = review.getOrderItemId();
                    Item item = null;
                    Order order = null;
                    try {
                        OrderItem orderItem = orderItemRepository.findById(Long.valueOf(orderItemId)).orElse(null);
                        if (orderItem != null) {
                            String itemId = orderItem.getItemId();
                            // relatedItems 중 해당 id 검색
                            item = relatedItems.stream()
                                    .filter(i -> i.getId().equals(itemId))
                                    .findFirst()
                                    .orElse(null);
                            // 주문일시 조회 위해 Order 객체도 가져오기
                            order = orderItem.getOrder();  // OrderItem에 Order 연관관계 있음
                        }
                    } catch (NumberFormatException ex) {
                        // orderItemId가 숫자가 아닐 경우 예외 처리 (없으면 null 유지)
                    }

                    // item이 이미 넘어온 relatedItems 단건(예: getReviewsByItemId)일 경우 처리 예외는 caller에서 조절
                    // 리뷰의 itemId와 relatedItems의 itemId가 1:1일 경우 item=null 대신 첫개 item 전달할 수도 있음

                    return new ReviewWithItemDto(review, item, maskedUsername,  order);
                })
                .collect(Collectors.toList());
    }

    /**
     * 구매자가 작성한 리뷰 목록 페이징 조회 (상품 정보 포함)
     */
    public Page<ReviewWithItemDto> getReviewsByBuyerId(String buyerId, Pageable pageable) {

        // 1. 리뷰 페이징 조회
        Page<Review> reviewPage = reviewRepository.findByBuyerId(buyerId, pageable);

        if (reviewPage.isEmpty()) {
            return Page.empty();
        }

        // 2. 리뷰에 포함된 itemId 리스트 추출 (중복 제거)
        List<String> itemIds = reviewPage.stream()
                .map(Review::getItemId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 관련 상품 리스트 조회
        List<Item> relatedItems = itemService.getItemsByIds(itemIds);

        // 4. DTO 변환
        List<ReviewWithItemDto> dtos = convertReviewsToDtos(reviewPage.getContent(), relatedItems);

        // 5. Page<ReviewWithItemDto>로 변환하여 반환
        return new PageImpl<>(dtos, pageable, reviewPage.getTotalElements());
    }

    /**
     * 작성 가능한 리뷰 목록 (배송 완료 & 미작성) 페이징 조회
     */
    public Page<OrderItemDto> getWritableReviews(String buyerId, Pageable pageable) {

        // 이미 리뷰 작성된 주문아이템 ID 리스트 조회
        List<String> reviewedOrderItemIdsStr = reviewRepository.findByBuyerId(buyerId).stream()
                .map(Review::getOrderItemId)
                .toList();

        List<Long> reviewedOrderItemIds = reviewedOrderItemIdsStr.isEmpty()
                ? List.of()
                : reviewedOrderItemIdsStr.stream()
                .map(Long::valueOf)
                .toList();

        // QueryDSL 커스텀 메서드 호출 (OrderItemRepositoryCustom)
        Page<OrderItem> deliveredOrderItems = orderItemRepository.findDeliveredUnreviewedOrderItems(Long.parseLong(buyerId), reviewedOrderItemIds, pageable);

        // 관련된 아이템 리스트를 한꺼번에 조회
        List<String> itemIds = deliveredOrderItems.stream()
                .map(OrderItem::getItemId)
                .distinct()
                .toList();
        List<Item> items = itemService.getItemsByIds(itemIds);

        // 아이템 아이디 -> 아이템 매핑
        Map<String, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        // DTO 변환 시 아이템 매핑과 함께 전달
        List<OrderItemDto> writableDtos = deliveredOrderItems.stream()
                .map(orderItem -> {
                    Item item = itemMap.get(orderItem.getItemId());
                    return new OrderItemDto(orderItem, item, item != null ? item.getImageUrl() : null);
                })
                .toList();

        return new PageImpl<>(writableDtos, pageable, deliveredOrderItems.getTotalElements());
    }

    public CountReviewDto countReviews(String buyerId) {
        // 본인이 작성한 리뷰 개수
        long writtenCount = reviewRepository.findByBuyerId(buyerId).size();

        // 본인이 작성 가능한 리뷰(배송 완료 & 미작성) 개수 반환
        List<String> reviewedOrderItemIdsStr = reviewRepository.findByBuyerId(buyerId).stream()
                .map(Review::getOrderItemId)
                .toList();

        List<Long> reviewedOrderItemIds = reviewedOrderItemIdsStr.isEmpty()
                ? List.of(-1L)
                : reviewedOrderItemIdsStr.stream().map(Long::valueOf).toList();

        // 페이징 없이 전체 개수 조회 (OrderItemRepository 커스텀 메서드 필요)
        long writableCount = orderItemRepository.countByBuyerIdAndDeliveryStatusAndIdNotIn(Long.parseLong(buyerId), DeliveryStatus.DELIVERED, reviewedOrderItemIds);
        return CountReviewDto.of(writableCount, writtenCount);
    }
}
