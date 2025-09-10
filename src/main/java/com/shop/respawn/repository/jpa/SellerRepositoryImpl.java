package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Role;
import com.shop.respawn.dto.query.FailureResultDto;
import com.shop.respawn.dto.query.UserQueryDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.shop.respawn.domain.QSeller.seller;

@Service
public class SellerRepositoryImpl implements SellerRepositoryCustom{

    private final BaseRepositoryImpl base; // 생성자 주입 권장

    @Override
    public Role findUserDtoRoleByUsername(String username) {
        return base.findUserDtoRoleByUsername(seller, seller.role, seller.username, username);
    }


    // 생성자에서 주입 받는 형태 권장
    public SellerRepositoryImpl(JPAQueryFactory queryFactory) {
        this.base = new BaseRepositoryImpl(queryFactory);
    }

    @Override
    public UserQueryDto findUserDtoByUsername(String username) {
        return base.findUserDtoByUsername(
                seller, seller.id, seller.name, seller.role, seller.username, username
        );
    }

    @Override
    public Optional<LocalDateTime> findLastPasswordChangedAtByUsername(String username) {
        return base.findLastPasswordChangedAt(
                seller, seller.username, seller.accountStatus.lastPasswordChangedAt, username
        );
    }

    @Override
    public long resetFailedLoginByUsername(String username) {
        return base.resetFailedLogin(
                seller, seller.username, seller.accountStatus.accountNonLocked, seller.accountStatus.failedLoginAttempts, username
        );
    }

    @Override
    public FailureResultDto increaseFailedAttemptsAndGetStatus(String username) {
        return base.increaseFailedAttemptsAndGetStatus(
                seller,
                seller.username,
                seller.accountStatus.enabled,
                seller.accountStatus.accountExpiryDate,
                seller.accountStatus.accountNonLocked,
                seller.accountStatus.failedLoginAttempts,
                username,
                5 // 임계치
        );
    }

}
