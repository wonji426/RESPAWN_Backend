package com.shop.respawn.controller;

import com.shop.respawn.dto.*;
import com.shop.respawn.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.shop.respawn.util.SessionUtil.getSellerIdFromSession;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰가 작성되었는지 확인하는 메서드
     */
    @GetMapping("/order-items/{orderItemId}")
    public ResponseEntity<?> checkReviewExists(
            @PathVariable String orderItemId,
            HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        if (buyerId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        boolean exists = reviewService.existsReviewByOrderItemId(buyerId, orderItemId);
        // exists가 true면 이미 리뷰 있음, false면 없음
        return ResponseEntity.ok(
                java.util.Map.of("reviewExists", exists)
        );
    }

    /**
     * 리뷰 작성 메서드
     */
    @PostMapping("/order-items/{orderItemId}")
    public ResponseEntity<?> createReview(
            @PathVariable String orderItemId,    // MongoDB의 ID형이 String이므로 String으로 바꿈
            @RequestBody @Valid ReviewRequestDto reviewRequestDto,
            HttpSession session) {
        try {
            Long buyerId = (Long) session.getAttribute("userId");
            if (buyerId == null) {
                return ResponseEntity.status(401).body("로그인이 필요합니다.");
            }

            reviewService.createReview(buyerId, orderItemId, reviewRequestDto.getRating(), reviewRequestDto.getContent());
            return ResponseEntity.ok("리뷰가 성공적으로 작성되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /**
     * 판매자가 자신이 판매한 아이템에 대한 리뷰 보기
     */
    @GetMapping("/seller/my-reviews")
    public ResponseEntity<List<ReviewWithItemDto>> getMyItemReviews(
            HttpSession session,
            @RequestParam(required = false) String itemId) {
        try {
            String sellerId = getSellerIdFromSession(session).toString();
            List<ReviewWithItemDto> reviews;
            if (itemId != null && !itemId.isEmpty()) {
                reviews = reviewService.getReviewsBySellerIdAndItemId(sellerId, itemId);
            } else {
                reviews = reviewService.getReviewsBySellerId(sellerId);
            }
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 리뷰 전체 조회: 특정 아이템의 리뷰
    @GetMapping("/items/{itemId}")
    public ResponseEntity<List<ReviewWithItemDto>> getReviewsByItemId(@PathVariable String itemId) {
        // 서비스에 위임
        List<ReviewWithItemDto> reviews = reviewService.getReviewsByItemId(itemId);
        return ResponseEntity.ok(reviews);
    }

    // [1] 본인 작성 리뷰 목록 페이징 조회
    /**
     * 구매자 본인 작성 리뷰 페이징 조회
     * /api/my-reviews/written?page=0&size=10
     */
    @GetMapping("/written")
    public ResponseEntity<Page<ReviewWithItemDto>> getWrittenReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewWithItemDto> reviewDtos = reviewService.getReviewsByBuyerId(authentication, pageable);
        return ResponseEntity.ok(reviewDtos);
    }

    // [2] 본인 작성 가능 리뷰 목록(배송완료+미작성) 페이징 조회
    @GetMapping("/writable")
    public ResponseEntity<Page<OrderItemDto>> getWritableReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OrderItemDto> result = reviewService.getWritableReviews(authentication, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 본인이 작성한 리뷰 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<CountReviewDto> getReviewCount(Authentication authentication) {
        CountReviewDto count = reviewService.countReviews(authentication);
        return ResponseEntity.ok(count);
    }

}