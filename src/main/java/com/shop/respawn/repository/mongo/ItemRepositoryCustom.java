package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.OffsetPage;

import java.util.List;

public interface ItemRepositoryCustom {

    List<Item> searchByKeywordRegex(String keyword);

    List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIdsHex);

    List<Item> fullTextSearch(String keyword);

    OffsetPage<Item> findItemsByOffsetUsingName(String categoryName, int offset, int limit);

}
