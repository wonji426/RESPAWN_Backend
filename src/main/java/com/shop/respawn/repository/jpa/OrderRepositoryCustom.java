package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Order;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryCustom {

    // 최근 한달간의 주문 조회 페이징 포함 주문 조회 메서드 시그니처 추가
    List<Order> findPaidOrdersByBuyerAndDateRange(Long buyerId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 최근 한달간의 주문 조회 주문 개수 조회용 메서드 추가
    long countPaidOrdersByBuyerAndDateRange(Long buyerId, LocalDateTime from, LocalDateTime to);

    /**
     * 구매자 ID와 ORDER ID 아이템 조회
     */
    Optional<Order> findByIdAndBuyerIdWithItems(Long orderId, Long buyerId);

}
