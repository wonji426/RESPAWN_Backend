package com.shop.respawn.dto;

import com.shop.respawn.domain.Item;

import java.util.List;

public record ItemCategoryDto(OffsetPage<Item> result, List<ItemDto> itemDtos) {
}