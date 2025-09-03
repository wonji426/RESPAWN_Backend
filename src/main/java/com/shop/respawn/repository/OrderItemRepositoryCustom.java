package com.shop.respawn.repository;

import com.shop.respawn.domain.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderItemRepositoryCustom {

    Page<OrderItem> findDeliveredUnreviewedOrderItems(Long buyerId, List<Long> excludeOrderItemIds, Pageable pageable);
}
