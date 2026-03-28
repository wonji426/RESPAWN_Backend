package com.shop.respawn.chatBot;

import com.shop.respawn.domain.ItemStatus;

public record SearchCondition(
        Integer maxPrice,
        Integer minPrice,
        String name,
        String deliveryType,
        Long deliveryFee,
        Long price,
        ItemStatus status,
        String company,
        String category,
        Long stockQuantity
) {}