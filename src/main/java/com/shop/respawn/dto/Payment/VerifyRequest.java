package com.shop.respawn.dto.Payment;

import lombok.Data;

import java.util.List;

@Data
public class VerifyRequest {
    private String impUid;
    private String merchantUid;
    private Long orderId;
    private Long selectedAddressId;
    private Long usePointAmount;
    private List<Long> selectedCartItemIds;
}
