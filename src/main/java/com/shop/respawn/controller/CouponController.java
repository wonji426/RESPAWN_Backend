package com.shop.respawn.controller;

import com.shop.respawn.dto.coupon.CouponValidationResult;
import com.shop.respawn.dto.coupon.OrderCouponCheckResponse;
import com.shop.respawn.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam("orderId") Long orderId,
            @RequestParam("code") String couponCode,
            Authentication authentication
    ) {

        CouponValidationResult result = couponService.checkApplicableForOrder(authentication, orderId, couponCode); // 서비스 위임 [9]

        if (!result.isOk()) {
            return ResponseEntity.ok(OrderCouponCheckResponse.fail(result.getMessage())); // HTTP 어댑팅 [9]
        }
        return ResponseEntity.ok(OrderCouponCheckResponse.ok()); // 성공 응답 [6][9]
    }
}
