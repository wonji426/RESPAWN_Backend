package com.shop.respawn.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponValidationResult {
    private final boolean ok;
    private final String message;

    public static CouponValidationResult ok() {
        return new CouponValidationResult(true, null); // 성공 상태만 반환 [9]
    }
    public static CouponValidationResult fail(String message) {
        return new CouponValidationResult(false, message); // 실패 사유 전달 [9]
    }
}
