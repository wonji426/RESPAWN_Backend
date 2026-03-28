package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Category;
import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.dto.item.ItemSummaryDto;
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
    public Page<Item> searchByKeywordAndCategories(String keyword, List<String> categoryIds, String company,
                                                   Long minPrice, Long maxPrice, String deliveryType, Pageable pageable) {
        Query query = new Query();

        // 1) 키워드 OR 조건
        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(buildKeywordOrRegex(keyword));
        }

        // 2) 카테고리 조건
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> cats = mongoTemplate.find(
                    Query.query(Criteria.where("name").in(categoryIds)),
                    Category.class, "categories"
            );
            List<ObjectId> catIds = cats.stream()
                    .map(c -> new ObjectId(c.getId().toString())).toList();

            if (catIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            query.addCriteria(Criteria.where("category").in(catIds));
        }

        // 3) 회사 조건, 4) 가격 조건, 5) 배송 조건 (기존과 동일하게 query에 criteria 추가)
        if (company != null && !company.isBlank()) query.addCriteria(Criteria.where("company").regex(company, "i"));
        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price");
            if (minPrice != null) priceCriteria.gte(minPrice);
            if (maxPrice != null) priceCriteria.lte(maxPrice);
            query.addCriteria(priceCriteria);
        }
        if (deliveryType != null && !deliveryType.isBlank()) {
            query.addCriteria(Criteria.where("deliveryType").is(deliveryType));
        }

        // 페이징 전 전체 카운트 조회
        long total = mongoTemplate.count(query, Item.class);

        // 페이징 설정 적용
        query.with(pageable);

        List<Item> items = mongoTemplate.find(query, Item.class);
        return new PageImpl<>(items, pageable, total);
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
    public Page<ItemDto> findSimpleItemsBySellerId(String sellerId,String search, Pageable pageable) {
        Criteria criteria = Criteria.where("sellerId").is(sellerId);

        if (search != null && !search.isBlank()) {
            criteria.and("name").regex(search, "i");
        }

        Query query = new Query(criteria).with(pageable);

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

        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, "item");

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

    @Override
    public List<ItemSummaryDto> findItemIdAndNameBySellerId(String sellerId) {
        Query query = new Query(Criteria.where("sellerId").is(sellerId));
        query.fields().include("_id").include("name");

        List<Document> docs = mongoTemplate.find(query, Document.class, "item");
        return docs.stream()
                .map(doc -> new ItemSummaryDto(
                        doc.getObjectId("_id").toString(),
                        doc.getString("name")
                ))
                .toList();
    }

}
