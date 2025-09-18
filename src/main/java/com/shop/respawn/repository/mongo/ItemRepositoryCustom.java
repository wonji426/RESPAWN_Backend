package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.Category;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.dto.item.ItemSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ItemRepositoryCustom {

    List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIdsHex);

    List<Item> fullTextSearch(String keyword);

    Page<ItemDto> findSimpleItemsBySellerId(String sellerId, Pageable pageable);

    Page<ItemDto> findItemsByCategoryWithPageable(String category, Pageable pageable);

    List<Item> findPartialItemsByIds(List<String> itemIds);

    Optional<Category> findCategoryByName(String name);

    List<ItemSummaryDto> findItemIdAndNameBySellerId(String sellerId);
}
