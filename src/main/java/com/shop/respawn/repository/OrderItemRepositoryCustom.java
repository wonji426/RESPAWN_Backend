package com.shop.respawn.repository;

import com.shop.respawn.domain.OrderItem;

import java.util.List;

public interface OrderItemRepositoryCustom {
    List<OrderItem> findDeliveredItemsWithOrderAndDeliveryByBuyerId(Long buyerId);

    List<OrderItem> findAllByIdInWithOrderAndDelivery(List<Long> ids);

    // 선택: 대량 데이터일 때 페이징 가능한 버전
    List<OrderItem> findDeliveredItemsByBuyerIdPaged(Long buyerId, int offset, int limit);

    long countDeliveredItemsByBuyerId(Long buyerId);
}
