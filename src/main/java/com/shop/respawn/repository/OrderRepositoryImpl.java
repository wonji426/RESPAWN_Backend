package com.shop.respawn.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
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
    public List<Order> findRecentPaidOrdersByBuyer(Long buyerId, LocalDateTime from, LocalDateTime to, int limit) {
        return queryFactory
                .selectFrom(order)
                .where(
                        order.buyer.id.eq(buyerId),
                        order.status.eq(OrderStatus.PAID),
                        order.orderDate.between(from, to)
                )
                .orderBy(order.orderDate.desc())
                .limit(limit)
                .fetch();
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
}