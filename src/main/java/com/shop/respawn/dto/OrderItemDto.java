package com.shop.respawn.dto;

import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.OrderItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderItemDto {

    private Long orderItemId;
    private String itemId;
    private String itemName; // 아이템 이름
    private String imageUrl;
    private Long orderId; // 주문아이디
    private Long orderPrice;
    private Long count;
    private LocalDateTime orderDate;
    private String deliveryStatus;

    public OrderItemDto(OrderItem orderItem, Item item, String imageUrl) {
        this.orderItemId = orderItem.getId();
        this.orderId = orderItem.getOrder().getId();
        this.itemId = orderItem.getItemId();
        this.itemName = item != null ? item.getName() : null;
        this.imageUrl = imageUrl;
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
