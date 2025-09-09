package com.shop.respawn.repository;

import com.shop.respawn.domain.Coupon;
import com.shop.respawn.dto.coupon.CouponStatusDto;

import java.util.List;

public interface CouponRepositoryCustom {
    List<Coupon> findAllUnusedByBuyerId(Long buyerId);

    List<CouponStatusDto> findAllByBuyerIdQueryDsl(Long buyerId);
}
