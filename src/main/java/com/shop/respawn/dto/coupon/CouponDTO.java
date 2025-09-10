package com.shop.respawn.dto.coupon;

import java.time.LocalDateTime;

import com.shop.respawn.domain.Coupon;
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

    public static CouponDTO fromEntity(Coupon coupon) {
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