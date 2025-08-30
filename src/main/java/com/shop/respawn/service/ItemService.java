package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.ItemDto;
import com.shop.respawn.dto.OffsetPage;
import com.shop.respawn.repository.ItemRepository;
import com.shop.respawn.repository.OrderItemRepository;
import com.shop.respawn.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.shop.respawn.domain.DeliveryStatus.*;
import static com.shop.respawn.domain.OrderStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final MongoTemplate mongoTemplate;
    private final ItemRepository itemRepository;
    private final SellerRepository sellerRepository;
    private final OrderItemRepository orderItemRepository; // 주문 아이템 조회용

    public Item registerItem(ItemDto itemDto, Long sellerId) {
        try {

            Seller findSeller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("판매자를 찾을 수 없습니다"));

            Item newItem = new Item();
            newItem.setName(itemDto.getName());
            newItem.setDeliveryType(itemDto.getDeliveryType());
            newItem.setDeliveryFee(itemDto.getDeliveryFee());
            newItem.setCompany(findSeller.getCompany());
            newItem.setCompanyNumber(findSeller.getCompanyNumber());
            newItem.setPrice(itemDto.getPrice());
            newItem.setStockQuantity(itemDto.getStockQuantity());
            newItem.setSellerId(String.valueOf(sellerId));
            newItem.setImageUrl(itemDto.getImageUrl()); // 대표 사진 경로만 저장
            newItem.setCategoryIds(itemDto.getCategoryIds());
            newItem.setDescription(itemDto.getDescription());
            if (newItem.getStatus() == null && ItemStatus.class.isEnum()) {
                newItem.setStatus(ItemStatus.SALE);
            }
            return itemRepository.save(newItem); // MongoDB에 저장
        } catch (Exception e) {
            System.err.println("상품 등록 실패: " + e.getMessage());
            throw new RuntimeException("상품 등록에 실패했습니다. [상세원인: " + e.getMessage() + "]", e);
        }
    }

    public Item updateItem(String itemId, ItemDto itemDto, Long sellerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));

        // 본인 상품인지 확인
        if (!item.getSellerId().equals(String.valueOf(sellerId))) {
            throw new RuntimeException("본인이 등록한 상품만 수정할 수 있습니다.");
        }

        // 상품 정보 수정
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setDeliveryType(itemDto.getDeliveryType());
        item.setDeliveryFee(itemDto.getDeliveryFee());
        item.setCompany(itemDto.getCompany());
        item.setCompanyNumber(itemDto.getCompanyNumber());
        item.setPrice(itemDto.getPrice());
        item.setStockQuantity(itemDto.getStockQuantity());
        item.setCategoryIds(itemDto.getCategoryIds());

        // 이미지 URL은 별도의 로직으로 처리하거나 그대로 유지
        if (itemDto.getImageUrl() != null && !itemDto.getImageUrl().isEmpty()) {
            item.setImageUrl(itemDto.getImageUrl());
        }

        return itemRepository.save(item);
    }

    public Item getItemById(String id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public OffsetPage<Item> findItemsByOffset(String category, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.min(Math.max(1, limit), 100);

        Query q = new Query();
        if (category != null && !category.isBlank()) {
            q.addCriteria(Criteria.where("categoryIds").is(category));
        }

        long total = mongoTemplate.count(q, Item.class);
        q.skip(safeOffset).limit(safeLimit);
        List<Item> items = mongoTemplate.find(q, Item.class);

        return new OffsetPage<>(items, total);
    }

    public List<Item> getItemsBySellerId(String sellerId) {
        return itemRepository.findBySellerId(sellerId);
    }

    public String getSellerIdByItemId(String itemId) {
        return itemRepository.findById(itemId)
                .map(Item::getSellerId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));
    }

    public List<Item> getItemsByIds(List<String> itemIds) {
        return itemRepository.findAllById(itemIds);
    }

    /**
     * 상품의 판매상태 조작 메서드
     */
    public void changeItemStatus(String itemId, Long sellerId, ItemStatus status) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        if (!item.getSellerId().equals(String.valueOf(sellerId))) {
            throw new RuntimeException("본인이 등록한 상품만 상태를 변경할 수 있습니다.");
        }
        item.setStatus(status);
        itemRepository.save(item);
    }

    public void deleteItemIfNoPendingDelivery(String itemId, Long sellerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));

        // 판매자 본인 상품인지 확인
        if (!item.getSellerId().equals(String.valueOf(sellerId))) {
            throw new RuntimeException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }

        // 해당 상품과 관련된 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findAllByItemId(itemId);

        for (OrderItem orderItem : orderItems) {
            Order order = orderItem.getOrder();
            Delivery delivery = orderItem.getDelivery();

            // 주문 상태가 결제 완료 혹은 주문 접수 상태이면서 배송이 완료 상태가 아니면 삭제 불가
            boolean paidOrder = order.getStatus() == ORDERED || order.getStatus() == PAID;
            boolean deliveryNotDone = delivery == null || delivery.getStatus() != DELIVERED;
            boolean itemStatus = item.getStatus() == ItemStatus.SALE;

            if (paidOrder && deliveryNotDone && itemStatus) {
                throw new RuntimeException("상품이 아직 판매 중 이거나 결제 완료된 주문이 배송 완료되지 않은 상품은 삭제할 수 없습니다.");
            }
        }

        // 모든 조건 통과 시 삭제 처리
        itemRepository.delete(item);
    }

    public List<Item> searchItems(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 키워드가 없으면 전체 조회 대신 빈 리스트 반환을 권장
            return List.of();
        }
        // 우선 정규식 기반 부분 일치
        // return itemRepository.searchByKeywordRegex(keyword.trim());
        // 텍스트 검색으로 전환하려면:
        return itemRepository.fullTextSearch(keyword.trim());
    }

    public List<Item> searchItemsByCategory(String keyword, List<String> categoryIds) {
        if ((keyword == null || keyword.isBlank()) && (categoryIds == null || categoryIds.isEmpty())) {
            return List.of();
        }
        if (categoryIds == null || categoryIds.isEmpty()) {
            return searchItems(keyword);
        }
        return itemRepository.searchByKeywordAndCategories(
                keyword == null ? "" : keyword.trim(),
                categoryIds
        );
    }
}
