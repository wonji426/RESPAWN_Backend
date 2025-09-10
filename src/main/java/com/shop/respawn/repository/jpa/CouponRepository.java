package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAllByBuyerId(Long buyerId);
}