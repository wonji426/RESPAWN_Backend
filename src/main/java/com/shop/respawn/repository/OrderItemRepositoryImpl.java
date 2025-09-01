package com.shop.respawn.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.shop.respawn.domain.DeliveryStatus.*;
import static com.shop.respawn.domain.QDelivery.delivery;
import static com.shop.respawn.domain.QOrder.order;
import static com.shop.respawn.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<OrderItem> findDeliveredItemsWithOrderAndDeliveryByBuyerId(Long buyerId) {
        return queryFactory
                .selectFrom(orderItem)
                .join(orderItem.order, order).fetchJoin()
                .join(orderItem.delivery, delivery).fetchJoin()
                .where(
                        order.buyer.id.eq(buyerId)
                                .and(delivery.status.eq(DELIVERED))
                )
                .orderBy(order.orderDate.desc())
                .fetch();
    }

    @Override
    public List<OrderItem> findAllByIdInWithOrderAndDelivery(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return queryFactory
                .selectFrom(orderItem)
                .join(orderItem.order, order).fetchJoin()
                .join(orderItem.delivery, delivery).fetchJoin()
                .where(orderItem.id.in(ids))
                .fetch();
    }

    @Override
    public List<OrderItem> findDeliveredItemsByBuyerIdPaged(Long buyerId, int offset, int limit) {
        return queryFactory
                .selectFrom(orderItem)
                .join(orderItem.order, order).fetchJoin()
                .join(orderItem.delivery, delivery).fetchJoin()
                .where(
                        order.buyer.id.eq(buyerId),
                        delivery.status.eq(DELIVERED)
                )
                .orderBy(order.orderDate.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countDeliveredItemsByBuyerId(Long buyerId) {
        // count 시 fetchJoin 제거, distinct 불필요(toOne만 조인)
        return queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .join(orderItem.order, order)
                .join(orderItem.delivery, delivery)
                .where(
                        order.buyer.id.eq(buyerId),
                        delivery.status.eq(DELIVERED)
                )
                .fetchOne();
    }
}
