package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Coupon;
import com.shop.respawn.dto.coupon.CouponStatusDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CouponRepositoryCustom {
    List<Coupon> findAllUnusedByBuyerId(Long buyerId);

    List<CouponStatusDto> findAllByBuyerIdQueryDsl(Long buyerId);

    Page<Coupon> findAllAvailableByBuyerId(Long buyerId, Pageable pageable);

    Page<Coupon> findAllUnavailableByBuyerId(Long buyerId, Pageable pageable);
}
