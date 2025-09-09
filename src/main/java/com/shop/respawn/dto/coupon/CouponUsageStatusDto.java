package com.shop.respawn.dto.coupon;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CouponUsageStatusDto {
    private CouponDTO coupon;
    private boolean usable;
    private String reason; // 사용 불가능 이유 (usable==false일 때 설명)

    public static CouponUsageStatusDto create(CouponDTO couponDTO, boolean usable, String reason) {
        CouponUsageStatusDto couponUsageStatusDto = new CouponUsageStatusDto();
        couponUsageStatusDto.setCoupon(couponDTO);
        couponUsageStatusDto.setUsable(usable);
        couponUsageStatusDto.setReason(reason);
        return couponUsageStatusDto;
    }
}