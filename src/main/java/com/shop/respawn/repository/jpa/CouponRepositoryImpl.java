package com.shop.respawn.repository.jpa;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Coupon;
import com.shop.respawn.dto.coupon.CouponStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.shop.respawn.domain.QCoupon.coupon;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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

    @Override
    public Page<Coupon> findAllAvailableByBuyerId(Long buyerId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        List<Coupon> result = queryFactory.selectFrom(coupon)
                .where(coupon.buyer.id.eq(buyerId)
                        .and(coupon.used.isFalse())
                        .and(coupon.expiresAt.after(now)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(Wildcard.count)
                .from(coupon)
                .where(coupon.buyer.id.eq(buyerId)
                        .and(coupon.used.isFalse())
                        .and(coupon.expiresAt.after(now)))
                .fetchOne();

        return new PageImpl<>(result, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Coupon> findAllUnavailableByBuyerId(Long buyerId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        List<Coupon> result = queryFactory.selectFrom(coupon)
                .where(coupon.buyer.id.eq(buyerId)
                        .and(coupon.used.isTrue()
                                .or(coupon.expiresAt.before(now).or(coupon.expiresAt.eq(now))))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(Wildcard.count)
                .from(coupon)
                .where(coupon.buyer.id.eq(buyerId)
                        .and(coupon.used.isTrue()
                                .or(coupon.expiresAt.before(now).or(coupon.expiresAt.eq(now))))
                )
                .fetchOne();

        return new PageImpl<>(result, pageable, total == null ? 0L : total);
    }
}
