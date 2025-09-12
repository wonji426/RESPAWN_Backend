package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.ItemDto;
import com.shop.respawn.dto.OffsetPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemRepositoryCustom {

    List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIdsHex);

    List<Item> fullTextSearch(String keyword);

    OffsetPage<Item> findItemsByOffsetUsingName(String categoryName, int offset, int limit);

    Page<ItemDto> findSimpleItemsBySellerId(String sellerId, Pageable pageable);

    Page<ItemDto> findItemsByCategoryWithPageable(String category, Pageable pageable);
}
