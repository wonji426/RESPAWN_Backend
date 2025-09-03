package com.shop.respawn.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponDTO {
    private Long id;
    private String code;
    private String name;
    private Long couponAmount;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private boolean used;

    public static CouponDTO fromEntity(com.shop.respawn.domain.Coupon coupon) {
        return new CouponDTO(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getCouponAmount(),
                coupon.getIssuedAt(),
                coupon.getExpiresAt(),
                coupon.isUsed()
        );
    }
}