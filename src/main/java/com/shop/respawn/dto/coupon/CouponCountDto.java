package com.shop.respawn.dto.coupon;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CouponCountDto {

    private int availableCount;
    private int unavailableCount;

    public CouponCountDto(int availableCount, int unavailableCount) {
        this.availableCount = availableCount;
        this.unavailableCount = unavailableCount;
    }
}
