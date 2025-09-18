package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.DeliveryStatus;
import com.shop.respawn.domain.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderItemRepositoryCustom {

    Page<OrderItem> findDeliveredUnreviewedOrderItems(Long buyerId, List<Long> excludeOrderItemIds, Pageable pageable);

    long countByBuyerIdAndDeliveryStatusAndIdNotIn(Long buyerId, DeliveryStatus status, List<Long> excludedOrderItemIds);

    List<OrderItem> findOrderItemsByOrderIds(List<Long> orderIds);

    Page<OrderItem> findRefundItemsByBuyer(Long buyerId, Pageable pageable);


}
