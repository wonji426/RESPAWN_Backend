package com.shop.respawn.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDto {

    private Long addressId;

    // 선택 주문 시 사용할 장바구니 아이템 ID 목록
    private List<Long> cartItemIds;

    // 상품 바로 주문용
    private String itemId;
    private Long count;

    // 포인트 사용 금액
    private Long usePointAmount;

    // 쿠폰 사용
    private String couponCode;
}
