package com.shop.respawn.repository;

import com.shop.respawn.domain.DeliveryStatus;
import com.shop.respawn.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {

    List<OrderItem> findAllByItemIdIn(List<String> itemIds);

    List<OrderItem> findAllByItemId(String itemId);

    List<OrderItem> findAllByItemIdInOrderByOrder_OrderDateDesc(List<String> itemIds);

    @Query("""
           SELECT oi
           FROM OrderItem oi
           JOIN oi.order o
           JOIN oi.delivery d
           WHERE o.buyer.id = :buyerId
             AND d.status = :status
           """)
    List<OrderItem> findDeliveredItemsByBuyerIdAndStatus(Long buyerId, DeliveryStatus status);

    List<OrderItem> findAllByOrder_IdIn(List<Long> orderIds);
}
