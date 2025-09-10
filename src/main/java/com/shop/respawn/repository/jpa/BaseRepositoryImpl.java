package com.shop.respawn.repository.jpa;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.dto.query.FailureResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Repository
@RequiredArgsConstructor
public class BaseRepositoryImpl implements BaseRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional
    public <E> FailureResultDto increaseFailedAttemptsAndGetStatus(
            EntityPathBase<E> root,
            StringPath usernamePath,
            BooleanPath enabledPath,
            DateTimePath<LocalDateTime> accountExpiryDatePath,
            BooleanPath nonLockedPath,
            NumberPath<Integer> failedAttemptsPath,
            String username,
            int lockThreshold
    ) {
        // 1) 실패횟수 +1 (잠금된 계정 제외)
        queryFactory.update(root)
                .where(usernamePath.eq(username), nonLockedPath.isTrue())
                .set(failedAttemptsPath, failedAttemptsPath.add(1))
                .execute();

        // 2) 임계치 도달 시 잠금
        queryFactory.update(root)
                .where(usernamePath.eq(username)
                        .and(failedAttemptsPath.goe(lockThreshold))
                        .and(nonLockedPath.isTrue()))
                .set(nonLockedPath, false)
                .execute();

        // 3) 상태 조회
        BooleanExpression expiredExpr = accountExpiryDatePath.before(LocalDateTime.now());
        Tuple tuple = queryFactory
                .select(enabledPath, expiredExpr, nonLockedPath.not(), failedAttemptsPath)
                .from(root)
                .where(usernamePath.eq(username))
                .fetchOne();

        Boolean enabledBox  = tuple != null ? tuple.get(enabledPath) : TRUE;
        Boolean expiredBox  = tuple != null ? tuple.get(expiredExpr) : null;
        Boolean lockedBox   = tuple != null ? tuple.get(nonLockedPath.not()) : null;
        Integer attemptsBox = tuple != null ? tuple.get(failedAttemptsPath) : null;

        boolean disabled = !TRUE.equals(enabledBox);
        boolean expired  = TRUE.equals(expiredBox);
        boolean locked   = TRUE.equals(lockedBox);
        int attempts     = attemptsBox != null ? attemptsBox : 0;

        return FailureResultDto.builder()
                .disabled(disabled)
                .expired(expired)
                .locked(locked)
                .failedAttempts(disabled ? 0 : attempts)
                .build();
    }

    @Override
    @Transactional
    public <E> long resetFailedLogin(
            EntityPathBase<E> root,
            StringPath usernamePath,
            BooleanPath nonLockedPath,
            NumberPath<Integer> failedAttemptsPath,
            String username
    ) {
        return queryFactory.update(root)
                .set(failedAttemptsPath, 0)
                .set(nonLockedPath, true)
                .where(usernamePath.eq(username))
                .execute();
    }

    @Override
    public <E> Optional<LocalDateTime> findLastPasswordChangedAt(
            EntityPathBase<E> root,
            StringPath usernamePath,
            DateTimePath<LocalDateTime> lastPasswordChangedAtPath,
            String username
    ) {
        LocalDateTime ts = queryFactory
                .select(lastPasswordChangedAtPath)
                .from(root)
                .where(usernamePath.eq(username))
                .fetchOne();
        return Optional.ofNullable(ts);
    }

    @Override
    public <E, I extends Number & Comparable<I>, R extends Enum<R>> UserQueryDto findUserDtoByUsername(
            EntityPathBase<E> root,
            NumberPath<I> idPath,
            StringPath namePath,
            EnumPath<R> rolePath,
            StringPath usernamePath,
            String username) {
        return queryFactory
                .select(Projections.constructor(UserQueryDto.class,
                        idPath,
                        namePath,
                        rolePath))
                .from(root)
                .where(usernamePath.eq(username))
                .fetchOne();
    }

    @Override
    public <E, R extends Enum<R>> R findUserDtoRoleByUsername(
            EntityPathBase<E> root,
            EnumPath<R> rolePath,
            StringPath usernamePath,
            String username
    ) {
        return queryFactory
                .select(rolePath)
                .from(root)
                .where(usernamePath.eq(username))
                .fetchOne();
    }

    @Override
    public <E> boolean existsUserIdentityConflict(
            EntityPathBase<E> root,
            StringPath emailPath,
            StringPath phoneNumberPath,
            StringPath usernamePath,
            String email,
            String phoneNumber,
            String username
    ) {
        Integer one = queryFactory
                .selectOne()
                .from(root)
                .where(
                        anyOf(
                                eqIfPresent(emailPath, email),
                                eqIfPresent(phoneNumberPath, phoneNumber),
                                eqIfPresent(usernamePath, username)
                        )
                )
                .fetchFirst();
        return one != null; // 하나라도 매칭되면 존재 [7][6]
    }

    private <T> BooleanExpression eqIfPresent(SimpleExpression<T> path, T value) {
        return value != null ? path.eq(value) : null; // null은 where에서 무시 [2][4]
    }

    private BooleanExpression anyOf(BooleanExpression... express) {
        BooleanExpression combined = null;
        for (BooleanExpression e : express) {
            if (e == null) continue;
            combined = (combined == null) ? e : combined.or(e);
        }
        return combined;
    }
}
