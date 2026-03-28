package com.shop.respawn.controller;

import com.shop.respawn.dto.*;
import com.shop.respawn.dto.review.CountReviewDto;
import com.shop.respawn.dto.review.ReviewExistsDto;
import com.shop.respawn.dto.review.ReviewRequestDto;
import com.shop.respawn.dto.review.ReviewWithItemDto;
import com.shop.respawn.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.shop.respawn.util.AuthenticationUtil.*;

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
            Authentication authentication,
            @PathVariable String orderItemId
            ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            boolean exists = reviewService.existsReviewByOrderItemId(buyerId, orderItemId);
            return ResponseEntity.ok(new ReviewExistsDto("reviewExists", exists)); // exists가 true면 이미 리뷰 있음, false면 없음
        } catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 리뷰 작성 메서드
     */
    @PostMapping("/order-items/{orderItemId}")
    public ResponseEntity<?> createReview(
            Authentication authentication,
            @PathVariable String orderItemId,    // MongoDB의 ID형이 String이므로 String으로 바꿈
            @RequestBody @Valid ReviewRequestDto reviewRequestDto
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
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
    public ResponseEntity<PageResponse<ReviewWithItemDto>> getMyItemReviews(
            Authentication authentication,
            @RequestParam(required = false) String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
            Pageable pageable = PageRequest.of(page, size, sortSpec);

            Page<ReviewWithItemDto> result;
            if (itemId != null && !itemId.isEmpty()) {
                result = reviewService.getReviewsBySellerIdAndItemId(String.valueOf(sellerId), itemId, pageable);
            } else {
                result = reviewService.getReviewsBySellerId(String.valueOf(sellerId), pageable);
            }
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    // 리뷰 전체 조회: 특정 아이템의 리뷰
    @GetMapping("/items/{itemId}")
    public ResponseEntity<PageResponse<ReviewWithItemDto>> getReviewsByItemId(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        try {
            Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
            Pageable pageable = PageRequest.of(page, size, sortSpec);
            Page<ReviewWithItemDto> result = reviewService.getReviewsByItemId(itemId, pageable);
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 구매자 본인 작성 리뷰 페이징 조회
     * /api/my-reviews/written?page=0&size=10
     */
    @GetMapping("/written")
    public ResponseEntity<PageResponse<ReviewWithItemDto>> getWrittenReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
            Pageable pageable = PageRequest.of(page, size, sortSpec);
            Page<ReviewWithItemDto> result = reviewService.getReviewsByBuyerId(String.valueOf(buyerId), pageable);
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (Exception e){
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    // [2] 본인 작성 가능 리뷰 목록(배송완료+미작성) 페이징 조회
    @GetMapping("/writable")
    public ResponseEntity<PageResponse<OrderItemDto>> getWritableReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Long buyerId = getUserIdFromAuthentication(authentication);
            Page<OrderItemDto> result = reviewService.getWritableReviews(String.valueOf(buyerId), pageable);
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (Exception e){
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 본인이 작성한 리뷰 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<CountReviewDto> getReviewCount(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        CountReviewDto count = reviewService.countReviews(String.valueOf(buyerId));
        return ResponseEntity.ok(count);
    }

}