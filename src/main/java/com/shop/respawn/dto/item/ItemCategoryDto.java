package com.shop.respawn.dto.item;

import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.OffsetPage;

import java.util.List;

public record ItemCategoryDto(OffsetPage<Item> result, List<ItemDto> itemDtos) {
}