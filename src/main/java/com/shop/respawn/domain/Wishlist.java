package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Buyer 엔티티와 다대일(ManyToOne) 매핑을 하거나, 식별자(buyerId)만 가질 수 있습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Buyer buyer;

    // MongoDB의 Item ID
    private String itemId;

    private LocalDateTime createdAt = LocalDateTime.now();
}