package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Category;
import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIds) {
        // 1) 키워드 OR 조건
        Criteria or = buildKeywordOrRegex(keyword == null ? "" : keyword);

        // 2) 카테고리 이름 → ObjectId 목록 매핑
        List<ObjectId> catIds = List.of();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            // categories 컬렉션에서 name IN으로 조회
            List<Category> cats = mongoTemplate.find(
                    Query.query(Criteria.where("name").in(categoryIds)),
                    Category.class,
                    "categories"
            );

            catIds = cats.stream()
                    .map(c -> {
                        Object idVal = c.getId(); // Category.id 타입이 ObjectId 또는 String일 수 있음
                        return new ObjectId(idVal.toString());
                    })
                    .toList();
        }

        // 3) 매핑 결과가 없으면 결과 없음 처리(오탐 방지)
        if (categoryIds != null && !categoryIds.isEmpty() && catIds.isEmpty()) {
            return List.of(); // 선택한 카테고리 이름과 일치하는 id가 없음 [9]
        }

        // 4) 카테고리 조건 결합 여부 결정
        if (catIds.isEmpty()) {
            // 카테고리 미선택 → 키워드만 검색
            return mongoTemplate.find(new Query(or), Item.class);
        }

        // 5) 키워드 OR ∧ category IN 결합
        Criteria cat = Criteria.where("category").in(catIds);
        Query q = new Query(new Criteria().andOperator(or, cat)); // $and 결합
        return mongoTemplate.find(q, Item.class);
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
    public Page<ItemDto> findSimpleItemsBySellerId(String sellerId, Pageable pageable) {
        Query query = new Query(Criteria.where("sellerId").is(sellerId)).with(pageable);
        query.fields()
                .include("_id")
                .include("name")
                .include("company")
                .include("imageUrl")
                .include("deliveryType")
                .include("price")
                .include("stockQuantity");
        List<Document> docs = mongoTemplate.find(query, Document.class, "item");
        List<ItemDto> list = docs.stream()
                .map(doc -> new ItemDto(
                        doc.getObjectId("_id").toString(),
                        doc.getString("name"),
                        doc.getString("company"),
                        doc.getString("imageUrl"),
                        doc.getString("deliveryType"),
                        doc.getLong("price"),
                        doc.getLong("stockQuantity")
                )).toList();
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), "item"); // 전체 개수
        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<ItemDto> findItemsByCategoryWithPageable(String category, Pageable pageable) {
        Query query = new Query();

        if (category != null && !category.isBlank()) {
            // 1) 카테고리 이름으로 카테고리 조회
            Category targetCategory = mongoTemplate.findOne(
                    Query.query(Criteria.where("name").is(category)),
                    Category.class, "categories"
            );
            if (targetCategory == null) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            // 2) 카테고리 ObjectId 기준으로 검색 조건 생성
            query.addCriteria(Criteria.where("category").is(new ObjectId(targetCategory.getId())));
        }

        // 3) 페이징 및 필요한 필드 프로젝션 적용
        query.with(pageable);
        query.fields()
                .include("_id")
                .include("name")
                .include("company")
                .include("price")
                .include("imageUrl");

        // 4) 전체 카운트 조회 (페이징 제외)
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Item.class, "item");

        // 5) 데이터 조회
        List<Item> items = mongoTemplate.find(query, Item.class, "item");

        // 6) ItemDto 변환
        List<ItemDto> itemDtos = items.stream()
                .map(item -> new ItemDto(
                        item.getId(),
                        item.getName(),
                        item.getCompany(),
                        item.getPrice(),
                        item.getImageUrl()
                ))
                .toList();

        // 7) PageImpl<ItemDto> 생성 및 반환
        return new PageImpl<>(itemDtos, pageable, total);
    }

    @Override
    public List<Item> findPartialItemsByIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        Query query = new Query(Criteria.where("_id").in(itemIds));
        // 필요한 필드만 프로젝션 설정 (id, name, imageUrl)
        query.fields()
                .include("_id")
                .include("name")
                .include("imageUrl");

        List<Document> docs = mongoTemplate.find(query, Document.class, "item");

        // Document -> Item 객체 매핑 (필요시 ItemDto로 변환하는 것도 가능)
        return docs.stream().map(doc -> {
            Item item = new Item();
            item.setId(doc.getObjectId("_id").toString());
            item.setName(doc.getString("name"));
            item.setImageUrl(doc.getString("imageUrl"));
            // 실제 Item 객체에 setter가 있다면, 또는 생성자 사용
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findCategoryByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Query query = Query.query(Criteria.where("name").is(name));
        Category category = mongoTemplate.findOne(query, Category.class, "categories");
        return Optional.ofNullable(category);
    }

}
