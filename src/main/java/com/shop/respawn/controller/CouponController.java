package com.shop.respawn.controller;

import com.shop.respawn.domain.Coupon;
import com.shop.respawn.dto.CouponDTO;
import com.shop.respawn.dto.coupon.CouponValidationResult;
import com.shop.respawn.dto.coupon.OrderCouponCheckResponse;
import com.shop.respawn.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/cancel")
    public ResponseEntity<OrderCouponCheckResponse> cancelOnOrder(
            @RequestParam("orderId") Long orderId,
            Authentication authentication
    ) {

        CouponValidationResult result = couponService.cancelApplicableForOrder(authentication, orderId);

        if (!result.isOk()) {
            return ResponseEntity.ok(OrderCouponCheckResponse.fail(result.getMessage()));
        }
        return ResponseEntity.ok(OrderCouponCheckResponse.ok()); // 성공 응답 [6][9]
    }

    /**
     * 구매자 ID로 쿠폰 목록 조회
     * @return 쿠폰 목록
     */
    @GetMapping("/view")
    public ResponseEntity<List<CouponDTO>> getCouponsByBuyerId(Authentication authentication) {
        List<CouponDTO> couponDTOs = couponService.getCouponDTOsByBuyerId(authentication);
        return ResponseEntity.ok(couponDTOs);
    }
}
