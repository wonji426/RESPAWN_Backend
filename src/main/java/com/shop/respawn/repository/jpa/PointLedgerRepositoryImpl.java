package com.shop.respawn.repository.jpa;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.PointLedger;
import com.shop.respawn.domain.PointTransactionType;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.shop.respawn.domain.QPointConsumeLink.pointConsumeLink;
import static com.shop.respawn.domain.QPointLedger.pointLedger;

@Repository
@RequiredArgsConstructor
public class PointLedgerRepositoryImpl implements PointLedgerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 미사용 적립분(SAVE) 후보 조회: 만료일 오름차순, 발생일 오름차순
     */
    @Override
    public List<PointLedger> findUsableSaveLedgers(Long buyerId, LocalDateTime now) {
        // SUM 서브쿼리를 BigDecimal 표현식으로 강제 래핑
        NumberExpression<BigDecimal> consumedSumExpr = getBigDecimalNumberExpression();
        //COALESCE(consumedSum, 0)
        NumberExpression<BigDecimal> consumedSumCoalesced = getConsumedSumCoalesced(consumedSumExpr);
        // NULLS LAST 보장: CASE WHEN expiryAt IS NULL THEN 1 ELSE 0 END ASC
        NumberExpression<Integer> expiryNullsLastKey =
                Expressions.numberTemplate(
                        Integer.class,
                        "case when {0} is null then 1 else 0 end",
                        pointLedger.expiryAt
                );
        return queryFactory
                .selectFrom(pointLedger)
                .where(
                        pointLedger.buyer.id.eq(buyerId),
                        pointLedger.type.eq(PointTransactionType.SAVE),
                        consumedSumCoalesced.lt(pointLedger.amount),
                        pointLedger.expiryAt.isNull().or(pointLedger.expiryAt.gt(now))
                )
                .orderBy(
                        new OrderSpecifier<>(Order.ASC, expiryNullsLastKey),
                        pointLedger.expiryAt.asc(),
                        pointLedger.occurredAt.asc(),
                        pointLedger.id.asc()
                )
                .fetch();
    }

    /**
     * 만료 대상 SAVE 조회
     */
    @Override
    public List<PointLedger> findExpireCandidates(Long buyerId, LocalDateTime now) {
        // 1) SUM 서브쿼리를 BigDecimal 표현식으로 래핑
        NumberExpression<BigDecimal> consumedSumExpr = getBigDecimalNumberExpression();
        // 2) COALESCE(consumedSum, 0)
        NumberExpression<BigDecimal> consumedSumCoalesced = getConsumedSumCoalesced(consumedSumExpr);
        return queryFactory
                .selectFrom(pointLedger)
                .where(
                        pointLedger.buyer.id.eq(buyerId),
                        pointLedger.type.eq(PointTransactionType.SAVE),
                        pointLedger.expiryAt.loe(now),              // expiryAt <= :now
                        consumedSumCoalesced.lt(pointLedger.amount) // 잔여 > 0
                )
                .orderBy(
                        pointLedger.expiryAt.asc(),
                        pointLedger.id.asc()
                )
                .fetch();
    }
    @Override
    public Page<PointLedger> findByBuyerAndTypes(Long buyerId, Iterable<PointTransactionType> types, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(pointLedger.buyer.id.eq(buyerId));
        if (types != null) {
            BooleanBuilder typeOr = new BooleanBuilder();
            for (PointTransactionType t : types) {
                typeOr.or(pointLedger.type.eq(t));
            }
            if (typeOr.hasValue()) {
                where.and(typeOr);
            }
        }

        return getPointLedgers(pageable, where);
    }

    @Override
    public Page<PointLedger> findAllByBuyer(Long buyerId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(pointLedger.buyer.id.eq(buyerId));

        return getPointLedgers(pageable, where);
    }

    @Override
    public List<PointLedger> findMonthlyExpireCandidates(Long buyerId, LocalDateTime monthStart, LocalDateTime monthEnd) {
        // 기존 BigDecimal 표현식 재사용 (consumedSum 서브쿼리 -> coalesce)
        NumberExpression<BigDecimal> consumedSumExpr = getBigDecimalNumberExpression();
        NumberExpression<BigDecimal> consumedSumCoalesced = getConsumedSumCoalesced(consumedSumExpr);

        return queryFactory
                .selectFrom(pointLedger)
                .where(
                        pointLedger.buyer.id.eq(buyerId),
                        pointLedger.type.eq(PointTransactionType.SAVE),
                        pointLedger.expiryAt.goe(monthStart),
                        pointLedger.expiryAt.loe(monthEnd),
                        consumedSumCoalesced.lt(pointLedger.amount) // 잔여 > 0
                )
                .orderBy(
                        pointLedger.expiryAt.asc(),
                        pointLedger.id.asc()
                )
                .fetch();
    }

    @Override
    public Long sumConsumedAmountOfSave(PointLedger saveLedger) {
        Long sum = queryFactory
                .select(pointConsumeLink.consumedAmount.sum())
                .from(pointConsumeLink)
                .where(pointConsumeLink.saveLedger.eq(saveLedger))
                .fetchOne();
        return sum == null ? 0L : sum;
    }

    @Override
    public Page<PointLedger> findByBuyerAndTypesAndOccurredBetween(Long buyerId, Iterable<PointTransactionType> types,
                                                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(pointLedger.buyer.id.eq(buyerId));
        if (types != null) {
            BooleanBuilder typeOr = new BooleanBuilder();
            for (PointTransactionType t : types) {
                typeOr.or(pointLedger.type.eq(t));
            }
            if (typeOr.hasValue()) {
                where.and(typeOr);
            }
        }
        where.and(pointLedger.occurredAt.between(from, to));
        return getPointLedgers(pageable, where);
    }

    @Override
    public Page<PointLedger> findAllByBuyerAndOccurredBetween(Long buyerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(pointLedger.buyer.id.eq(buyerId));
        where.and(pointLedger.occurredAt.between(from, to));
        return getPointLedgers(pageable, where);
    }

    @NotNull
    private static NumberExpression<BigDecimal> getBigDecimalNumberExpression() {
        // SUM 서브쿼리를 BigDecimal 표현식으로 래핑
        return Expressions.numberTemplate(
                BigDecimal.class,
                "{0}",
                JPAExpressions
                        .select(pointConsumeLink.consumedAmount.sum())
                        .from(pointConsumeLink)
                        .where(pointConsumeLink.saveLedger.eq(pointLedger))
        );
    }

    @NotNull
    private static NumberExpression<BigDecimal> getConsumedSumCoalesced(NumberExpression<BigDecimal> consumedSumExpr) {
        // COALESCE(consumedSum, 0)
        return Expressions.numberTemplate(
                BigDecimal.class,
                "coalesce({0}, {1})",
                consumedSumExpr,
                BigDecimal.ZERO
        );
    }

    @NotNull
    private PageImpl<PointLedger> getPointLedgers(Pageable pageable, BooleanBuilder where) {
        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());

        List<PointLedger> content = queryFactory
                .selectFrom(pointLedger)
                .where(where)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(pointLedger.count())
                .from(pointLedger)
                .where(where)
                .fetchOne();

        long totalCount = (total == null) ? 0L : total;
        return new PageImpl<>(content, pageable, totalCount);
    }

    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 화이트리스트: 허용 정렬 필드만 매핑
        Map<String, ComparableExpressionBase<?>> sortable = new HashMap<>();
        sortable.put("occurredAt", pointLedger.occurredAt);
        sortable.put("expiryAt", pointLedger.expiryAt);
        sortable.put("amount", pointLedger.amount);
        sortable.put("id", pointLedger.id);
        sortable.put("type", pointLedger.type); // 필요 시

        if (sort != null) {
            for (Sort.Order o : sort) {
                ComparableExpressionBase<?> expr = sortable.get(o.getProperty());
                if (expr != null) {
                    orders.add(new OrderSpecifier<>(
                            o.isAscending() ? Order.ASC : Order.DESC,
                            expr
                    ));
                }
            }
        }

        // 기본 정렬: occurredAt desc, id desc
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, pointLedger.occurredAt));
            orders.add(new OrderSpecifier<>(Order.DESC, pointLedger.id));
        }
        return orders;
    }
}
