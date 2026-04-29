package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Temporary {

    @Id @GeneratedValue
    @Column(name = "temporary_id")
    private Long id;

    @Column
    private Long orderId;

    // 사용자가 쓰겠다고 요청한 포인트 (조작 방지용 임시 저장)
    @Column(columnDefinition = "bigint default 0")
    private Long usedPointAmount;

    @Column
    private Long addressId;

    @Column
    private String couponCode;
}
