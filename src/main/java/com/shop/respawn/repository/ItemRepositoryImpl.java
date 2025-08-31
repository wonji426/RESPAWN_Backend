package com.shop.respawn.repository;

import com.shop.respawn.domain.Category;
import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final MongoTemplate mongoTemplate;

    private Criteria buildKeywordOrRegex(String keyword) {
        String escaped = java.util.regex.Pattern.quote(keyword);
        return new Criteria().orOperator(
                Criteria.where("name").regex(escaped, "i"),
                Criteria.where("description").regex(escaped, "i"),
                Criteria.where("company").regex(escaped, "i")
        );
    }

    @Override
    public List<Item> searchByKeywordRegex(String keyword) {
        Query q = new Query(buildKeywordOrRegex(keyword));
        return mongoTemplate.find(q, Item.class);
    }

    @Override
    public List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIds) {
        Criteria or = buildKeywordOrRegex(keyword);
        Criteria cat = Criteria.where("categoryIds").in(categoryIds);
        Query q = new Query(new Criteria().andOperator(or, cat));
        return mongoTemplate.find(q, Item.class);
    }

    @Override
    public List<Item> fullTextSearch(String keyword) {
        TextCriteria text = TextCriteria.forDefaultLanguage().matching(keyword);
        Query q = TextQuery.queryText(text).sortByScore();
        return mongoTemplate.find(q, Item.class);
    }

    @Override
    public OffsetPage<Item> findItemsByOffsetUsingName(String categoryName, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.min(Math.max(1, limit), 100);

        Query query = new Query();

        if (categoryName != null && !categoryName.isBlank()) {
            // 1. 이름으로 카테고리 찾기
            Category targetCategory = mongoTemplate.findOne(
                    Query.query(Criteria.where("name").is(categoryName)),
                    Category.class, "categories"
            );

            if (targetCategory == null) {
                // 이름과 일치하는 카테고리가 없으면 빈 결과 반환
                return new OffsetPage<>(List.of(), 0L);
            }

            // 2. 찾은 카테고리 ID(String)를 ObjectId로 변환하여 조회
            query.addCriteria(Criteria.where("category").is(new ObjectId(targetCategory.getId())));
        }

        long total = mongoTemplate.count(query, Item.class, "item");
        query.skip(safeOffset).limit(safeLimit);

        List<Item> items = mongoTemplate.find(query, Item.class, "item");
        return new OffsetPage<>(items, total);
    }
}
