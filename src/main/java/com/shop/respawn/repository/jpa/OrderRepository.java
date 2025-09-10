package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    List<Order> findByBuyer_IdOrderByOrderDateDesc(Long buyerId);

    Order findTop1ByBuyer_IdAndStatusOrderByOrderDateDesc(Long buyerId, OrderStatus status);

    List<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status);

    /**
     * 구매자 ID와 주문 상태 목록, 결제 상태로 주문 조회
     */
    List<Order> findByBuyerIdAndStatusInAndPaymentStatus(Long buyerId, List<OrderStatus> statuses, String paymentStatus);

}
