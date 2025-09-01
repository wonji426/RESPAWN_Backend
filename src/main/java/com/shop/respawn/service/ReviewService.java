package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.OffsetPage;
import com.shop.respawn.dto.ReviewLite;
import com.shop.respawn.dto.ReviewWithItemDto;
import com.shop.respawn.dto.WritableReviewDto;
import com.shop.respawn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ItemRepository itemRepository;

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
    public List<ReviewWithItemDto> getReviewsBySellerId(String sellerId) {
        List<Item> sellerItems = itemService.getItemsBySellerId(sellerId);
        List<String> sellerItemIds = sellerItems.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        List<Long> orderItemIds = orderItemRepository.findAllByItemIdIn(sellerItemIds).stream()
                .map(OrderItem::getId)
                .toList();

        if (orderItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Review> reviews = reviewRepository.findByOrderItemIdInOrderByCreatedDateDesc(
                orderItemIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList())
        );

        // 리뷰 변환을 공통 메서드로 위임 (sellerItems 전달)
        return convertReviewsToDtos(reviews, sellerItems);
    }

    /**
     * 판매자 ID와 특정 아이템 ID로 필터링된 리뷰 리스트 조회
     */
    public List<ReviewWithItemDto> getReviewsBySellerIdAndItemId(String sellerId, String itemId) {
        // 판매자 상품 리스트 가져오기
        List<Item> sellerItems = itemService.getItemsBySellerId(sellerId);

        // 해당 itemId가 판매자 상품 목록에 있는지 체크 (보안/유효성 검사)
        boolean ownsItem = sellerItems.stream()
                .anyMatch(item -> item.getId().equals(itemId));
        if (!ownsItem) {
            // 판매자가 판매하지 않는 상품이면 빈 리스트 반환
            return Collections.emptyList();
        }

        // 특정 상품에 해당하는 주문 아이템 ID만 조회
        List<Long> orderItemIds = orderItemRepository.findAllByItemId(itemId).stream()
                .map(OrderItem::getId)
                .toList();

        if (orderItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Review> reviews = reviewRepository.findByOrderItemIdInOrderByCreatedDateDesc(
                orderItemIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList())
        );

        // 리뷰 변환 (기존 메서드 활용)
        return convertReviewsToDtos(reviews, sellerItems);
    }

    // 특정 아이템(itemId)에 대한 모든 리뷰 가져오기
    public List<ReviewWithItemDto> getReviewsByItemId(String itemId) {
        List<Review> reviews = reviewRepository.findByItemIdOrderByCreatedDateDesc(itemId);

        Item item = itemService.getItemById(itemId);
        // 단일 상품이므로 리스트로 만들어 전달
        List<Item> singleItemList = List.of(item);

        // 리뷰 변환 공통 메서드 사용
        return convertReviewsToDtos(reviews, singleItemList);
    }

    public boolean existsReviewByOrderItemId(Long buyerId, String orderItemId) {
        // 본인 리뷰만 체크(옵션), buyerId 체크 생략하면 공용 체크
        return reviewRepository.findByOrderItemId(orderItemId)
                .filter(review -> review.getBuyerId().equals(String.valueOf(buyerId)))
                .isPresent();
    }

    private List<ReviewWithItemDto> convertReviewsToDtos(List<Review> reviews, List<Item> relatedItems) {
        return reviews.stream()
                .map(review -> {
                    // 리뷰 작성자 ID
                    String buyerId = review.getBuyerId();
                    String maskedUsername;

                    try {
                        String buyerUsername = buyerRepository.findById(Long.valueOf(buyerId))
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

                    ReviewWithItemDto reviewWithItemDto = new ReviewWithItemDto(review, item, maskedUsername,  order);
                    System.out.println("reviewWithItemDto = " + reviewWithItemDto);
                    return reviewWithItemDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 작성 가능 여부
     */
    @Transactional(readOnly = true)
    public List<WritableReviewDto> getWritableReviews(Authentication authentication) {
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());

        // 1) 배송 완료 OrderItem + Order + Delivery를 fetch join으로 한 번에
        List<OrderItem> deliveredOrderItems =
                orderItemRepository.findDeliveredItemsWithOrderAndDeliveryByBuyerId(buyerId);

        if (deliveredOrderItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 리뷰 존재 여부를 MongoDB에서 in 한 번으로
        List<String> orderItemIds = deliveredOrderItems.stream()
                .map(oi -> String.valueOf(oi.getId()))
                .toList();

        List<Review> existingReviews = reviewRepository.findByOrderItemIdIn(orderItemIds);
        Set<String> reviewedOrderItemIdSet = existingReviews.stream()
                .map(Review::getOrderItemId)
                .collect(Collectors.toSet());

        // 3) 필요한 아이템만 모아 MongoDB에서 배치 조회
        List<String> itemIds = deliveredOrderItems.stream()
                .map(OrderItem::getItemId)
                .distinct()
                .toList();

        List<Item> items = itemRepository.findAllById(itemIds);
        Map<String, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, it -> it));

        // 4) 스트림 변환 (exists는 Set 조회로 O(1))
        return deliveredOrderItems.stream()
                .filter(orderItem -> !reviewedOrderItemIdSet.contains(String.valueOf(orderItem.getId())))
                .map(orderItem -> {
                    Item item = itemMap.get(orderItem.getItemId());
                    boolean exists = reviewedOrderItemIdSet.contains(String.valueOf(orderItem.getId()));
                    return new WritableReviewDto(
                            orderItem.getOrder().getId(),
                            String.valueOf(orderItem.getId()),
                            item != null ? item.getName() : null,
                            item != null ? item.getImageUrl() : null,
                            exists
                    );
                })
                .toList();
    }

    /**
     * 리뷰 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewWithItemDto> getWrittenReviews(Authentication authentication) {
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());

        // 1) 리뷰 목록(프로젝션 사용 가능)
        List<ReviewLite> reviews = reviewRepository.findByBuyerIdOrderByCreatedDateDesc(String.valueOf(buyerId));

        if (reviews.isEmpty()) return List.of();

        // 2) MongoDB 아이템 배치 조회
        List<String> itemIds = reviews.stream()
                .map(ReviewLite::getItemId)
                .distinct()
                .toList();
        List<Item> items = itemRepository.findAllById(itemIds);
        Map<String, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, it -> it));

        // 3) OrderItem + Order + Delivery를 QueryDSL fetch join으로 배치 조회
        List<Long> orderItemIds = reviews.stream()
                .map(ReviewLite::getOrderItemId)
                .map(Long::valueOf)
                .distinct()
                .toList();

        List<OrderItem> orderItems = orderItemRepository.findAllByIdInWithOrderAndDelivery(orderItemIds);
        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, oi -> oi));

        // 4) DTO 매핑 (convertReviewsToDtos 내부에서 개별 조회하지 않도록 개선)
        return reviews.stream()
                .map(rv -> {
                    Item item = itemMap.get(rv.getItemId());
                    OrderItem oi = orderItemMap.get(Long.valueOf(rv.getOrderItemId()));
                    Order ord = (oi != null) ? oi.getOrder() : null;

                    String maskedUsername;
                    try {
                        String buyerUsername = buyerRepository.findById(Long.valueOf(rv.getBuyerId()))
                                .map(Buyer::getUsername)
                                .orElse("알 수 없는 사용자");
                        maskedUsername = maskMiddleFourChars(buyerUsername);
                    } catch (NumberFormatException e) {
                        maskedUsername = "알 수 없는 사용자";
                    }

                    return new ReviewWithItemDto(rv, item, maskedUsername, ord);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OffsetPage<WritableReviewDto> getWritableReviewsPaged(Authentication authentication, int offset, int limit) {
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());

        long total = orderItemRepository.countDeliveredItemsByBuyerId(buyerId);
        if (total == 0) return new OffsetPage<>(List.of(), 0L);

        List<OrderItem> deliveredOrderItems =
                orderItemRepository.findDeliveredItemsByBuyerIdPaged(buyerId, offset, limit);

        List<String> orderItemIds = deliveredOrderItems.stream()
                .map(oi -> String.valueOf(oi.getId()))
                .toList();

        List<Review> existingReviews = reviewRepository.findByOrderItemIdIn(orderItemIds);
        Set<String> reviewed = existingReviews.stream()
                .map(Review::getOrderItemId)
                .collect(Collectors.toSet());

        List<String> itemIds = deliveredOrderItems.stream()
                .map(OrderItem::getItemId)
                .distinct()
                .toList();
        Map<String, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, it -> it));

        List<WritableReviewDto> content = deliveredOrderItems.stream()
                .filter(oi -> !reviewed.contains(String.valueOf(oi.getId())))
                .map(oi -> {
                    Item item = itemMap.get(oi.getItemId());
                    boolean exists = reviewed.contains(String.valueOf(oi.getId()));
                    return new WritableReviewDto(
                            oi.getOrder().getId(),
                            String.valueOf(oi.getId()),
                            item != null ? item.getName() : null,
                            item != null ? item.getImageUrl() : null,
                            exists
                    );
                }).toList();

        return new OffsetPage<>(content, total);
    }
}
