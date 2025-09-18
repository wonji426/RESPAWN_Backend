package com.shop.respawn.dto.user;

import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerOrderDto {
    private Long orderId;               // 주문번호
    private Long orderItemId;           // 주문 아이템 id
    private String itemId;
    private String buyerName;           // 구매자 이름
    private String itemName;            // 상품명
    private Long count;                  // 수량
    private Long totalPrice;             // 결제 금액
    private String orderStatus;         // 주문 상태
    private LocalDateTime orderDate;    // 주문 일시

    public SellerOrderDto(Order order, OrderItem orderItem, Item item) {
        this.orderId = order.getId();
        this.orderItemId = orderItem.getId();
        this.itemId = item.getId();
        this.buyerName = order.getBuyer().getName();
        this.itemName = item.getName();
        this.count = orderItem.getCount();
        this.totalPrice = orderItem.getOrderPrice() * orderItem.getCount();
        this.orderStatus = order.getStatus().name();
        this.orderDate = order.getOrderDate();
    }
}
