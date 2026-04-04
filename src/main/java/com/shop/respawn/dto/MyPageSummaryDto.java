package com.shop.respawn.dto;

import com.shop.respawn.domain.Grade;
import lombok.Data;

@Data
public class MyPageSummaryDto {
    private Long orderCount;
    private Long couponCount;
    private Long wishlistCount;
    private Long reviewCount;
    private Grade grade;
    private Long activePoint;

    public MyPageSummaryDto(Long orderCount, Long couponCount, Long wishlistCount, Long reviewCount, Grade grade, Long activePoint) {
        this.orderCount = orderCount;
        this.couponCount = couponCount;
        this.wishlistCount = wishlistCount;
        this.reviewCount = reviewCount;
        this.grade = grade;
        this.activePoint = activePoint;
    }
}
