package com.shop.respawn.controller;

import com.shop.respawn.dto.coupon.*;
import com.shop.respawn.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.shop.respawn.util.AuthenticationUtil.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    /**
     * 주문 페이지에서 쿠폰 사용 가능 여부 확인 (아이템 합계 기준, 배송비 제외)
     * - 조건: Σ(orderPrice * count) > couponAmount
     * - 쿠폰 사용(markUsed) 하지 않음. 결제 완료 시점에 apply 처리.
     * 예: GET /api/coupons/check?orderId=1&code=XXXX-....
     */
    @GetMapping("/check")
    public ResponseEntity<OrderCouponCheckResponse> checkOnOrder(
            Authentication authentication,
            @RequestParam("orderId") Long orderId,
            @RequestParam("code") String couponCode
    ) {

        Long buyerId = getUserIdFromAuthentication(authentication);

        CouponValidationResult result = couponService.checkApplicableForOrder(buyerId, orderId, couponCode); // 서비스 위임 [9]

        if (!result.isOk()) {
            return ResponseEntity.ok(OrderCouponCheckResponse.fail(result.getMessage())); // HTTP 어댑팅
        }
        return ResponseEntity.ok(OrderCouponCheckResponse.ok()); // 성공 응답
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOnOrder(
            Authentication authentication,
            @RequestParam("orderId") Long orderId
    ) {

        Long buyerId = getUserIdFromAuthentication(authentication);

        CouponValidationResult result = couponService.cancelApplicableForOrder(buyerId, orderId);

        if (!result.isOk()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(HttpStatus.NO_CONTENT); // 성공 응답 [6][9]
    }

    /**
     * 구매자 ID로 쿠폰 목록 조회
     * @return 쿠폰 목록
     */
    @GetMapping("/view")
    public ResponseEntity<List<CouponUsageStatusDto>> getCouponsByBuyerIdAndOrder(
            Authentication authentication,
            @RequestParam Long orderId
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        List<CouponUsageStatusDto> couponsStatus = couponService.getCouponsUsageStatusByBuyerAndOrder(buyerId, orderId);
        return ResponseEntity.ok(couponsStatus);
    }

    /**
     * 쿠폰 개수 반환
     * GET /api/coupons/count
     */
    @GetMapping("/count")
    public ResponseEntity<CouponCountDto> getCouponCount(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        CouponCountDto count = couponService.countCouponsByBuyerId(buyerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/available")
    public ResponseEntity<Page<CouponDTO>> getAvailableCoupons(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CouponDTO> coupons = couponService.getAvailableCouponsByBuyerId(buyerId, pageable);
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/unavailable")
    public ResponseEntity<Page<CouponDTO>> getUnavailableCoupons(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<CouponDTO> coupons = couponService.getUnavailableCouponsByBuyerId(buyerId, pageable);
        return ResponseEntity.ok(coupons);
    }
}
