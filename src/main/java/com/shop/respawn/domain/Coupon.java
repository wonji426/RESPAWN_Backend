package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id @GeneratedValue
    @Column(name = "coupon_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buyer_id")
    private Buyer buyer;

    @Column(nullable = false, unique = true, length = 36)
    private String code;

    @Column(nullable = false)
    private String name; // 쿠폰 이름 (예: "Gold 등급 축하 쿠폰")

    @Column(nullable = false)
    private Long couponAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    public void markUsed() {
        this.used = true;
    }

    public static Coupon createCoupon(Buyer buyer, String name, Long couponAmount, LocalDateTime expiresAt) {
        if (buyer == null) {
            throw new IllegalArgumentException("buyer는 null일 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 비어 있을 수 없습니다.");
        }
        if (couponAmount == null || couponAmount <= 0L) {
            throw new IllegalArgumentException("couponAmount는 0보다 커야 합니다.");
        }
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("expiresAt은 현재 시각 이후여야 합니다.");
        }

        return Coupon.builder()
                .buyer(buyer)
                .code(java.util.UUID.randomUUID().toString()) // length=36, unique 충족
                .name(name)
                .couponAmount(couponAmount)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .used(false)
                .build();
    }
}
