package com.shop.respawn.repository.jpa;

import com.querydsl.core.types.dsl.*;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.dto.query.FailureResultDto;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BaseRepository {

    <E> FailureResultDto increaseFailedAttemptsAndGetStatus(
            EntityPathBase<E> root,
            StringPath usernamePath,
            BooleanPath enabledPath,
            DateTimePath<LocalDateTime> accountExpiryDatePath,
            BooleanPath nonLockedPath,
            NumberPath<Integer> failedAttemptsPath,
            String username,
            int lockThreshold
    );

    <E> long resetFailedLogin(
            EntityPathBase<E> root,
            StringPath usernamePath,
            BooleanPath nonLockedPath,
            NumberPath<Integer> failedAttemptsPath,
            String username
    );

    <E> Optional<LocalDateTime> findLastPasswordChangedAt(
            EntityPathBase<E> root,
            StringPath usernamePath,
            DateTimePath<LocalDateTime> lastPasswordChangedAtPath,
            String username
    );

    <E, I extends Number & Comparable<I>, R extends Enum<R>> UserQueryDto findUserDtoByUsername(
            EntityPathBase<E> root,
            NumberPath<I> idPath,
            StringPath namePath,
            EnumPath<R> rolePath,
            StringPath usernamePath,
            String username);

    <E, R extends Enum<R>> R findUserDtoRoleByUsername(
            EntityPathBase<E> root,
            EnumPath<R> rolePath,
            StringPath usernamePath,
            String username);

    <E> boolean existsUserIdentityConflict(
            EntityPathBase<E> root,
            StringPath emailPath,
            StringPath phoneNumberPath,
            StringPath usernamePath,
            String email,
            String phoneNumber,
            String username);
}
