package com.shop.respawn.repository;

import com.shop.respawn.domain.Coupon;

import java.util.List;

public interface CouponRepositoryCustom {
    List<Coupon> findAllUnusedByBuyerId(Long buyerId);
}
