package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import static com.shop.respawn.domain.QDelivery.delivery;
import static com.shop.respawn.domain.QOrder.order;
import static com.shop.respawn.domain.QOrderItem.orderItem;
import static com.shop.respawn.domain.QRefund.refund;

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

    @Override
    public Page<OrderItem> findRefundItemsByBuyer(Long buyerId, Pageable pageable) {
        // 환불 상태: REQUESTED/REFUNDED, 주문 상태: ORDERED/PAID 만
        var statuses = List.of(RefundStatus.REQUESTED, RefundStatus.REFUNDED);

        List<OrderItem> content = queryFactory
                .selectFrom(orderItem)
                .leftJoin(orderItem.delivery, delivery).fetchJoin()
                .leftJoin(orderItem.order).fetchJoin()
                .where(
                        orderItem.order.buyer.id.eq(buyerId),
                        orderItem.order.status.in(OrderStatus.ORDERED, OrderStatus.PAID),
                        orderItem.refundStatus.in(statuses)
                )
                // 최신 요청 우선 정렬: 환불 요청 엔티티가 있다면 requestedAt, 없으면 orderItem.id 기준
                .orderBy(orderItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .where(
                        orderItem.order.buyer.id.eq(buyerId),
                        orderItem.order.status.in(OrderStatus.ORDERED, OrderStatus.PAID),
                        orderItem.refundStatus.in(statuses)
                )
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<OrderItem> findRefundItemsBySellerItemIds(Set<String> sellerItemIds, RefundStatus status, Pageable pageable) {
        // 서비스에서 빈 집합은 미리 처리
        List<OrderItem> content = queryFactory
                .selectFrom(orderItem)
                .leftJoin(orderItem.order, order).fetchJoin()
                .leftJoin(orderItem.delivery, delivery).fetchJoin()
                .leftJoin(orderItem.refund, refund).fetchJoin()
                .where(
                        orderItem.refundStatus.eq(status),
                        orderItem.itemId.in(sellerItemIds)
                )
                .orderBy(refund.requestedAt.desc().nullsLast(), orderItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .leftJoin(orderItem.refund, refund)
                .where(
                        orderItem.refundStatus.eq(status),
                        orderItem.itemId.in(sellerItemIds)
                )
                .fetchOne();

        long total = (totalCount == null) ? 0L : totalCount;
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<OrderItem> findSellerOrderItemsPage(Set<String> sellerItemIds, Pageable pageable) {
        if (sellerItemIds == null || sellerItemIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // content 쿼리: 필요한 연관만 fetch join
        List<OrderItem> content = queryFactory
                .selectFrom(orderItem)
                .leftJoin(orderItem.order, order).fetchJoin()
                .leftJoin(orderItem.delivery, delivery).fetchJoin()
                .where(orderItem.itemId.in(sellerItemIds),
                        order.status.ne(OrderStatus.TEMPORARY))
                .orderBy(order.orderDate.desc(), orderItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리: fetch join 제외
        Long totalCount = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .leftJoin(orderItem.order, order)
                .where(orderItem.itemId.in(sellerItemIds),
                        order.status.ne(OrderStatus.TEMPORARY))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;
        return new PageImpl<>(content, pageable, total);
    }

}
