package com.shop.respawn.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartItemIdsRequest {
    private List<Long> cartItemIds;
}
