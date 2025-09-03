package com.shop.respawn.dto;

import com.shop.respawn.domain.OrderItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderItemDto {

    private Long orderItemId;
    private String itemId;
    private Long orderPrice;
    private Long count;
    private LocalDateTime orderDate;
    private String deliveryStatus;

    public OrderItemDto(OrderItem orderItem) {
        this.orderItemId = orderItem.getId();
        this.itemId = orderItem.getItemId();
        this.orderPrice = orderItem.getOrderPrice();
        this.count = orderItem.getCount();

        if (orderItem.getOrder() != null) {
            this.orderDate = orderItem.getOrder().getOrderDate();
        }

        if (orderItem.getDelivery() != null) {
            this.deliveryStatus = orderItem.getDelivery().getStatus().name();
        }
    }
}
