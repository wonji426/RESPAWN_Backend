package com.shop.respawn.repository;

import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Role;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.dto.query.FailureResultDto;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.shop.respawn.domain.QBuyer.buyer;
import static com.shop.respawn.domain.QCoupon.coupon;
import static com.shop.respawn.domain.QPointBalance.pointBalance;

@Repository
public class BuyerRepositoryImpl implements BuyerRepositoryCustom {

    private final BaseRepositoryImpl base; // 생성자 주입 권장
    private final JPAQueryFactory queryFactory;

    // 생성자에서 주입 받는 형태 권장
    public BuyerRepositoryImpl(JPAQueryFactory queryFactory) {
        this.base = new BaseRepositoryImpl(queryFactory);
        this.queryFactory = queryFactory;
    }

    @Override
    public Role findUserDtoRoleByUsername(String username) {
        return base.findUserDtoRoleByUsername(buyer, buyer.role, buyer.username, username);
    }

    @Override
    public UserQueryDto findUserDtoByUsername(String username) {
        return base.findUserDtoByUsername(
                buyer, buyer.id, buyer.name, buyer.role, buyer.username, username
        );
    }

    @Override
    public Optional<LocalDateTime> findLastPasswordChangedAtByUsername(String username) {
        return base.findLastPasswordChangedAt(
                buyer, buyer.username, buyer.accountStatus.lastPasswordChangedAt, username
        );
    }

    @Override
    public long resetFailedLoginByUsername(String username) {
        return base.resetFailedLogin(
                buyer, buyer.username, buyer.accountStatus.accountNonLocked, buyer.accountStatus.failedLoginAttempts, username
        );
    }

    @Override
    public FailureResultDto increaseFailedAttemptsAndGetStatus(String username) {
        return base.increaseFailedAttemptsAndGetStatus(
                buyer,
                buyer.username,
                buyer.accountStatus.enabled,
                buyer.accountStatus.accountExpiryDate,
                buyer.accountStatus.accountNonLocked,
                buyer.accountStatus.failedLoginAttempts,
                username,
                5
        );
    }

    @Override
    public boolean existsUserIdentityConflict(String email, String phoneNumber, String username) {
        return base.existsUserIdentityConflict(
                buyer,
                buyer.email,
                buyer.phoneNumber,
                buyer.username,
                email,
                phoneNumber,
                username
        );
    }

    // 사용가능 포인트
    @Override
    public Long findActivePoint(Long buyerId) {
        Long ActivePoint = queryFactory
                .select(pointBalance.active)
                .from(pointBalance)
                .where(pointBalance.buyerId.eq(buyerId))
                .fetchOne();
        return ActivePoint != null ? ActivePoint : 0L;
    }

    // 사용가능 쿠폰 개수(미사용 + 유효기간 남음)
    @Override
    public Long countUsableCoupons(Long buyerId, LocalDateTime now) {
        Long coupons = queryFactory
                .select(Wildcard.count) // select count(*)
                .from(coupon)
                .where(
                        coupon.buyer.id.eq(buyerId),
                        coupon.used.isFalse(),
                        coupon.expiresAt.after(now)
                )
                .fetchOne();
        return coupons != null ? coupons : 0L;
    }

    // 현재 등급
    @Override
    public Grade findBuyerGrade(Long buyerId) {
        return queryFactory
                .select(buyer.grade)
                .from(buyer)
                .where(buyer.id.eq(buyerId))
                .fetchOne();
    }

    @Override
    public Long findOnlyBuyerIdByUsername(String username) {
        return queryFactory
                .select(buyer.id)
                .from(buyer)
                .where(buyer.username.eq(username))
                .fetchOne();
    }

    @Override
    public UserQueryDto findUserGradeById(Long buyerId) {
        return (UserQueryDto) queryFactory
                .select(buyer.username, buyer.grade)
                .from(buyer)
                .where(buyer.id.eq(buyerId))
                .fetchOne();
    }
}
