package com.shop.respawn.dto.refund;

import lombok.Data;

@Data
public class RefundResponse {

    private Long expirePoint;
    private Long cancelUsePoint;
    private String content;

    public RefundResponse(Long expirePoint, Long cancelUsePoint, String content) {
        this.expirePoint = expirePoint;
        this.cancelUsePoint = cancelUsePoint;
        this.content = content;
    }

    public RefundResponse(String content) {
        this.content = content;
    }
}
