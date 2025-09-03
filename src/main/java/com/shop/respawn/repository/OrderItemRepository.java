package com.shop.respawn.repository;

import com.shop.respawn.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {

    List<OrderItem> findAllByItemIdIn(List<String> itemIds);

    List<OrderItem> findAllByItemId(String itemId);

    List<OrderItem> findAllByItemIdInOrderByOrder_OrderDateDesc(List<String> itemIds);

    List<OrderItem> findAllByOrder_IdIn(List<Long> orderIds);

}
