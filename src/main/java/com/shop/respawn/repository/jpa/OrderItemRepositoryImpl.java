package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.DeliveryStatus;
import com.shop.respawn.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.shop.respawn.domain.QDelivery.delivery;
import static com.shop.respawn.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderItem> findDeliveredUnreviewedOrderItems(Long buyerId, List<Long> excludeOrderItemIds, Pageable pageable) {
        // 메인 쿼리: 배송 완료 & 본인 주문이면서 제외 리스트에 속하지 않는 주문아이템 조회
        List<OrderItem> content = queryFactory
                .selectFrom(orderItem)
                .where(
                        orderItem.order.buyer.id.eq(buyerId),
                        orderItem.delivery.status.eq(DeliveryStatus.DELIVERED),
                        orderItem.id.notIn(excludeOrderItemIds.isEmpty() ? List.of(-1L) : excludeOrderItemIds)
                )
                .orderBy(orderItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리: 전체 개수 조회
        Long totalCount = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .where(
                        orderItem.order.buyer.id.eq(buyerId),
                        orderItem.delivery.status.eq(DeliveryStatus.DELIVERED),
                        orderItem.id.notIn(excludeOrderItemIds.isEmpty() ? List.of(-1L) : excludeOrderItemIds)
                )
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long countByBuyerIdAndDeliveryStatusAndIdNotIn(Long buyerId, DeliveryStatus status, List<Long> excludedOrderItemIds) {
        if (excludedOrderItemIds == null || excludedOrderItemIds.isEmpty()) {
            excludedOrderItemIds = List.of(-1L); // 빈 리스트 방지용 임시 값
        }

        Long count = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .leftJoin(orderItem.delivery, delivery)
                .where(
                        orderItem.order.buyer.id.eq(buyerId),
                        delivery.status.eq(status),
                        orderItem.id.notIn(excludedOrderItemIds)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderIds(List<Long> orderIds) {
        return queryFactory.selectFrom(orderItem)
                .where(orderItem.order.id.in(orderIds))
                .fetch();
    }
}
