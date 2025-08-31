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

import static java.util.regex.Pattern.quote;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final MongoTemplate mongoTemplate;

    private Criteria buildKeywordOrRegex(String keyword) {
        String escaped = quote(keyword == null ? "" : keyword);
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
        Criteria or = buildKeywordOrRegex(keyword == null ? "" : keyword);
        List<ObjectId> catIds = categoryIds == null ? List.of() :
                categoryIds.stream().filter(ObjectId::isValid).map(ObjectId::new).toList();
        if (catIds.isEmpty()) return mongoTemplate.find(new Query(or), Item.class);
        Criteria cat = Criteria.where("category").in(catIds);
        return mongoTemplate.find(new Query(new Criteria().andOperator(or, cat)), Item.class);
    }

    @Override
    public List<Item> fullTextSearch(String keyword) {
        try {
            TextCriteria text = TextCriteria.forDefaultLanguage().matching(keyword == null ? "" : keyword);
            Query q = TextQuery.queryText(text).sortByScore();
            return mongoTemplate.find(q, Item.class);
        } catch (Exception ex) {
            // 텍스트 인덱스 부재/오류 시 폴백
            Query q = new Query(buildKeywordOrRegex(keyword == null ? "" : keyword));
            return mongoTemplate.find(q, Item.class);
        }
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
