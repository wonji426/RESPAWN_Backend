package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.ItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemRepositoryCustom {

    List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIdsHex);

    List<Item> fullTextSearch(String keyword);

    Page<ItemDto> findSimpleItemsBySellerId(String sellerId, Pageable pageable);

    Page<ItemDto> findItemsByCategoryWithPageable(String category, Pageable pageable);

    List<Item> findPartialItemsByIds(List<String> itemIds);
}
