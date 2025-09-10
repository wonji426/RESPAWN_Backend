package com.shop.respawn.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCouponCheckResponse {
    private boolean usable;
    private String reason;

    public static OrderCouponCheckResponse ok() { return new OrderCouponCheckResponse(true, null); }
    public static OrderCouponCheckResponse fail(String reason) { return new OrderCouponCheckResponse(false, reason); }
}
