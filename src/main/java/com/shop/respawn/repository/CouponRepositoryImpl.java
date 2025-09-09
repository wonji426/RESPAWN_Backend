package com.shop.respawn.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Coupon;
import com.shop.respawn.dto.coupon.CouponStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.shop.respawn.domain.QCoupon.coupon;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Coupon> findAllUnusedByBuyerId(Long buyerId) {

        return queryFactory.selectFrom(coupon)
                .where(coupon.buyer.id.eq(buyerId)
                        .and(coupon.used.isFalse()))
                .fetch();
    }

    @Override
    public List<CouponStatusDto> findAllByBuyerIdQueryDsl(Long buyerId) {
        return queryFactory
                .select(Projections.constructor(
                        CouponStatusDto.class,
                        coupon.used,
                        coupon.expiresAt
                ))
                .from(coupon)
                .where(coupon.buyer.id.eq(buyerId))
                .fetch();
    }
}
