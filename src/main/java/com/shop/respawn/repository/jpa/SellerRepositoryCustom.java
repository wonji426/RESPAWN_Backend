package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Role;
import com.shop.respawn.dto.query.FailureResultDto;
import com.shop.respawn.dto.query.UserQueryDto;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SellerRepositoryCustom {
    UserQueryDto findUserDtoByUsername(String username);

    Optional<LocalDateTime> findLastPasswordChangedAtByUsername(String username);

    long resetFailedLoginByUsername(String username);

    FailureResultDto increaseFailedAttemptsAndGetStatus(String username);

    Role findUserDtoRoleByUsername(String username);
}
