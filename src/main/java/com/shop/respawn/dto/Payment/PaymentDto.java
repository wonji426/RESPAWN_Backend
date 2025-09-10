package com.shop.respawn.dto.Payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDto {
    private String impUid;
    private Long amount;
    private String pgProvider;
    private String paymentMethod;
    private String cardName;
    private String status;
    private String name;
    private String merchantUid;
    private Long buyerId;  // 추가
    private Long orderId;  // 추가
    private Long usePointAmount;
}
