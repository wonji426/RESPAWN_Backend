package com.shop.respawn.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.QPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.shop.respawn.domain.QPayment.payment;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom{

    private final JPAQueryFactory queryFactory;


    @Override
    public Long sumMonthlyAmountByBuyer(Long buyerId, LocalDateTime start, LocalDateTime end) {
        return queryFactory
                .select(payment.amount.sum().coalesce(0L))
                .from(payment)
                .where(payment.buyer.id.eq(buyerId)
                        .and(payment.status.eq("paid"))
                        .and(payment.createdAt.between(start, end)))
                .fetchOne();
    }
}
