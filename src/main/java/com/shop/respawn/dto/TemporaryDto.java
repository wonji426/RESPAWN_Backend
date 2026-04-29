package com.shop.respawn.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TemporaryDto {
    private Long orderId;
    private Long usePointAmount;
    private Long addressId;
    private String couponCode;
}
