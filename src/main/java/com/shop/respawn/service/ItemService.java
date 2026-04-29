package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.dto.item.ItemSummaryDto;
import com.shop.respawn.repository.mongo.CategoryRepository;
import com.shop.respawn.repository.mongo.ItemRepository;
import com.shop.respawn.repository.jpa.OrderItemRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.shop.respawn.domain.DeliveryStatus.*;
import static com.shop.respawn.domain.OrderStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final SellerRepository sellerRepository;
    private final OrderItemRepository orderItemRepository; // 주문 아이템 조회용
    private final CategoryRepository categoryRepository;

    public Item registerItem(ItemDto itemDto, Long sellerId) {
        try {

            Seller findSeller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("판매자를 찾을 수 없습니다"));

            Category category = itemRepository.findCategoryByName(itemDto.getCategoryName())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다: " + itemDto.getCategory()));

            ObjectId categoryId = new ObjectId(category.getId());

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
            newItem.setCategory(categoryId);
            newItem.setDescription(itemDto.getDescription());
            newItem.setCreatedAt(LocalDateTime.now());
            newItem.setSoldCount(0L);
            newItem.setWishCount(0L);
            newItem.setReviewCount(0L);
            if (newItem.getStatus() == null && ItemStatus.class.isEnum()) {
                newItem.setStatus(ItemStatus.SALE);
            }
            return itemRepository.save(newItem); // MongoDB에 저장
        } catch (Exception e) {
            System.err.println("상품 등록 실패: " + e.getMessage());
            throw new RuntimeException("상품 등록에 실패했습니다. [상세원인: " + e.getMessage() + "]", e);
        }
    }

    public Item updateItem(String itemId, ItemDto itemDto, String sellerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));

        // 본인 상품인지 확인
        if (!item.getSellerId().equals(sellerId)) {
            throw new RuntimeException("본인이 등록한 상품만 수정할 수 있습니다.");
        }

        Category category = itemRepository.findCategoryByName(itemDto.getCategoryName())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다: " + itemDto.getCategory()));

        ObjectId categoryId = new ObjectId(category.getId());

        // 상품 정보 수정
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setDeliveryType(itemDto.getDeliveryType());
        item.setDeliveryFee(itemDto.getDeliveryFee());
        item.setCompany(itemDto.getCompany());
        item.setCompanyNumber(itemDto.getCompanyNumber());
        item.setPrice(itemDto.getPrice());
        item.setStockQuantity(itemDto.getStockQuantity());
        item.setCategory(categoryId);

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

    public Page<ItemDto> getItemByCategory(String category, Pageable pageable) {
        Page<ItemDto> itemPage = itemRepository.findItemsByCategoryWithPageable(category, pageable);

        List<ItemDto> itemDtos = itemPage.stream()
                .map(item -> new ItemDto(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        item.getDeliveryType(),
                        item.getDeliveryFee(),
                        item.getCompany(),
                        item.getCompanyNumber(),
                        item.getPrice(),
                        item.getStockQuantity(),
                        item.getSellerId(),
                        item.getImageUrl(),
                        item.getCategory(),
                        item.getSoldCount(),
                        item.getReviewCount()
                ))
                .toList();

        return new PageImpl<>(itemDtos, pageable, itemPage.getTotalElements());
    }

    public List<Item> getItemsBySellerId(String sellerId) {
        return itemRepository.findBySellerId(sellerId);
    }

    public Page<ItemDto> getSimpleItemsBySellerId(String sellerId,String search, Pageable pageable) {
        return itemRepository.findSimpleItemsBySellerId(sellerId, search, pageable);
    }

    public List<Item> getPartialItemsByIds(List<String> itemIds) {
        return itemRepository.findPartialItemsByIds(itemIds);
    }

    public String getSellerIdByItemId(String itemId) {
        return itemRepository.findById(itemId)
                .map(Item::getSellerId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));
    }

    public List<Item> getItemsByIds(List<String> itemIds) {
        return itemRepository.findAllById(itemIds);
    }

    @Transactional(readOnly = true)
    public List<ItemSummaryDto> getMyItemIdAndNames(String sellerId) {
        return itemRepository.findItemIdAndNameBySellerId(sellerId);
    }

    /**
     * 상품의 판매상태 조작 메서드
     */
    public void changeItemStatus(String itemId, String sellerId, ItemStatus status) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        if (!item.getSellerId().equals(sellerId)) {
            throw new RuntimeException("본인이 등록한 상품만 상태를 변경할 수 있습니다.");
        }
        item.setStatus(status);
        itemRepository.save(item);
    }

    public void deleteItemIfNoPendingDelivery(String itemId, String sellerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));

        // 판매자 본인 상품인지 확인
        if (!item.getSellerId().equals(sellerId)) {
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

    /**
     * 고급 검색 페이징(키워드 + 카테고리 + 회사명 + 가격 범위 + 배송 방법)
     */
    public Page<ItemDto> searchItemsByCategory(String query, List<String> categoryIds, String company,
                                               Long minPrice, Long maxPrice, String deliveryType, Pageable pageable) {

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("최소 가격이 최대 가격보다 클 수 없습니다.");
        }

        Page<Item> itemPage = itemRepository.searchByKeywordAndCategories(
                query, categoryIds, company, minPrice, maxPrice, deliveryType, pageable
        );

        // Page 내부의 Item들을 ItemDto로 변환
        return itemPage.map(item -> new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getDeliveryType(),
                item.getDeliveryFee(),
                item.getCompany(),
                item.getCompanyNumber(),
                item.getPrice(),
                item.getStockQuantity(),
                item.getSellerId(),
                item.getImageUrl(),
                item.getCategory(),
                null,    // categoryName (필요 없으면 null)
                item.getStatus(),
                item.getWishCount(), // wishCount 포함
                item.getSoldCount(), // soldCount 포함
                item.getReviewCount()
        ));
    }

    public ItemDto findItemWithCategoryName(String id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 상품을 찾을 수 없습니다. id=" + id));

        String categoryName = null;
        ObjectId categoryId = item.getCategory();

        if (categoryId != null) {
            Optional<Category> categoryOptional = categoryRepository.findById(categoryId.toString());
            if (categoryOptional.isPresent()) {
                categoryName = categoryOptional.get().getName();
            }
        }

        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getDeliveryType(),
                item.getDeliveryFee(),
                item.getCompany(),
                item.getCompanyNumber(),
                item.getPrice(),
                item.getStockQuantity(),
                item.getSellerId(),
                item.getImageUrl(),
                item.getCategory(),
                categoryName,
                item.getStatus(),
                item.getWishCount(),
                item.getSoldCount(),
                item.getReviewCount()
        );
    }

    /**
     * Item 엔티티를 직접 저장/업데이트 합니다. (찜 카운트 업데이트 등에 사용)
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
    * 판매량 증가
    */
    public void increaseSoldCount(String itemId, long quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));
        item.addSoldCount(quantity);
        itemRepository.save(item);
    }
}
