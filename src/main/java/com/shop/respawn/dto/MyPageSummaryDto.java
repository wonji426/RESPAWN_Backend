package com.shop.respawn.dto;

import lombok.Data;

@Data
public class MyPageSummaryDto {
    private Long orderCount;
    private Long couponCount;
    private Long wishlistCount;
    private Long reviewCount;

    public MyPageSummaryDto(Long orderCount, Long couponCount, Long wishlistCount, Long reviewCount) {
        this.orderCount = orderCount;
        this.couponCount = couponCount;
        this.wishlistCount = wishlistCount;
        this.reviewCount = reviewCount;
    }
}
