package com.shop.respawn.dto.order;

import com.shop.respawn.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderSummaryDto {
    private Long orderId;
    private String orderName;          // 예: "크리에이티브 PEBBLE V3 외 1건"
    private String representativeImageUrl; // 대표 이미지 URL
    private LocalDateTime orderDate;
    private Long totalAmount;
    private OrderStatus status;
}