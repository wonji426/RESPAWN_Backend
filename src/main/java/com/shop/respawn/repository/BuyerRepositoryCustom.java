package com.shop.respawn.repository;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Role;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.dto.query.FailureResultDto;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BuyerRepositoryCustom {
    UserQueryDto findUserDtoByUsername(String username);

    Optional<LocalDateTime> findLastPasswordChangedAtByUsername(String username);

    long resetFailedLoginByUsername(String username);

    FailureResultDto increaseFailedAttemptsAndGetStatus(String username);

    Role findUserDtoRoleByUsername(String username);

    boolean existsUserIdentityConflict(String email, String phoneNumber, String username);

    // 사용가능 포인트
    Long findActivePoint(Long buyerId);

    // 사용가능 쿠폰 개수(미사용 + 유효기간 남음)
    Long countUsableCoupons(Long buyerId, LocalDateTime now);

    // 현재 등급
    Grade findBuyerGrade(Long buyerId);

    Long findOnlyBuyerIdByUsername(String username);

    UserQueryDto findUserGradeByUsername(String username);

    UserQueryDto findUserGradeById(Long buyerId);
}
