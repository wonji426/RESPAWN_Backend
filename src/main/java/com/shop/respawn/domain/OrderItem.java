package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "order_item")
@Getter @Setter
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order; //주문

    private String itemId; //아이템 객체가 몽고디비 사용
    private Long orderPrice; //주문 가격
    private Long count; //주문 수량

    @OneToOne(cascade = ALL, fetch = LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // 환불 상태 추가 (예: REFUNDED, REQUESTED, NONE 등)
    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @OneToOne(mappedBy = "orderItem", cascade = ALL, orphanRemoval = true)
    private Refund refund;

    //==생성 메서드==//
    public static OrderItem createOrderItem(Item item, Long orderPrice, Long count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);
        return orderItem;
    }

}
