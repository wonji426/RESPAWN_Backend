package com.shop.respawn.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDto {
    private String impUid;
    private Long amount;
    private String status;
    private String name;
    private String merchantUid;
    private Long buyerId;  // 추가
    private Long orderId;  // 추가
}
