package com.shop.respawn.controller;

import com.shop.respawn.dto.CouponDTO;
import com.shop.respawn.dto.coupon.CouponValidationResult;
import com.shop.respawn.dto.coupon.OrderCouponCheckResponse;
import com.shop.respawn.service.CouponService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<CouponDTO>> getCouponsByBuyerId(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        List<CouponDTO> couponDTOs = couponService.getCouponDTOsByBuyerId(buyerId);
        return ResponseEntity.ok(couponDTOs);
    }
}
