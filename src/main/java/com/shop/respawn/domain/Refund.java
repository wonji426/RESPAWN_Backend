package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "refund")
@Getter @Setter
public class Refund {
    @Id @GeneratedValue
    @Column(name = "refund_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buyer_id")
    private Buyer buyer;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "order_item_id", unique = true)
    private OrderItem orderItem;

    private String refundReason;

    @Column(columnDefinition = "TEXT")
    private String refundDetail;

    private LocalDateTime requestedAt;

}