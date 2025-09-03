package com.shop.respawn.repository;

import com.shop.respawn.domain.Coupon;
import com.shop.respawn.domain.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAllByBuyerId(Long buyerId);
}