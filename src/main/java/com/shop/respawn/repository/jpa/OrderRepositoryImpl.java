package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.shop.respawn.domain.QOrder.*;
import static com.shop.respawn.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Order> findPaidOrdersByBuyerAndDateRange(Long buyerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return queryFactory.selectFrom(order)
                .where(order.buyer.id.eq(buyerId)
                        .and(order.orderDate.between(from, to))
                        .and(order.status.eq(OrderStatus.PAID)))
                .orderBy(order.orderDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countPaidOrdersByBuyerAndDateRange(Long buyerId, LocalDateTime from, LocalDateTime to) {
        Long count = queryFactory.select(order.count())
                .from(order)
                .where(order.buyer.id.eq(buyerId)
                        .and(order.orderDate.between(from, to))
                        .and(order.status.eq(OrderStatus.PAID)))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public Optional<Order> findByIdAndBuyerIdWithItems(Long orderId, Long buyerId) {
        Order result = queryFactory
                .select(order)
                .distinct()
                .from(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .where(
                        order.id.eq(orderId),
                        order.buyer.id.eq(buyerId)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Order> findOrdersByBuyerOrderByDateDesc(Long buyerId, Pageable pageable) {
        return queryFactory.selectFrom(order)
                .where(order.buyer.id.eq(buyerId))
                .orderBy(order.orderDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countOrdersByBuyer(Long buyerId) {
        Long count = queryFactory.select(order.count())
                .from(order)
                .where(order.buyer.id.eq(buyerId))
                .fetchOne();
        return count != null ? count : 0L;
    }

}