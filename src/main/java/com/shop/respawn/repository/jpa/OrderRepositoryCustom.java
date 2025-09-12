package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryCustom {

    /**
     * 최근 한달간의 주문 조회
     */
    List<Order> findPaidOrdersByBuyerAndDateRange(Long buyerId, LocalDateTime from, LocalDateTime to);

    /**
     * 구매자 ID와 ORDER ID 아이템 조회
     */
    Optional<Order> findByIdAndBuyerIdWithItems(Long orderId, Long buyerId);
}
