package com.shop.respawn.controller;

import com.shop.respawn.dto.*;
import com.shop.respawn.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
     * 자신이 작성한 리뷰 조회 및 리뷰 작성 가능 여부
     */
    @GetMapping("/my")
    public ResponseEntity<MyReviewsResponse> getMyReviews(Authentication authentication) {
        // 서비스가 모든 조회/최적화를 담당
        List<WritableReviewDto> writableItems = reviewService.getWritableReviews(authentication); // 배송완료 + 미작성 목록
        List<ReviewWithItemDto> writtenReviews = reviewService.getWrittenReviews(authentication); // 작성한 리뷰 목록

        MyReviewsResponse body = new MyReviewsResponse(writableItems, writtenReviews);
        return ResponseEntity.ok(body);
    }

    // 작성 가능 리뷰 페이징
    @GetMapping("/my/writable")
    public ResponseEntity<WritableReviewsPageResponse> getMyWritableReviewsPaged(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int offset,   // 시작 인덱스[27]
            @RequestParam(defaultValue = "20") int limit    // 최대 개수[27]
    ) {
        // 서비스의 페이징 메서드 호출
        OffsetPage<WritableReviewDto> page = reviewService.getWritableReviewsPaged(authentication, offset, limit); // 총합 포함
        return ResponseEntity.ok(new WritableReviewsPageResponse(page.items(), page.total())); // 간단 DTO로 래핑
    }

    @GetMapping("/my/written")
    public ResponseEntity<WrittenReviewsPageResponse> getMyWrittenReviewsPaged(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        OffsetPage<ReviewWithItemDto> page = reviewService.getWrittenReviewsPaged(authentication, offset, limit); // 서비스 호출
        return ResponseEntity.ok(new WrittenReviewsPageResponse(page.items(), page.total())); // items/total 응답
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

}