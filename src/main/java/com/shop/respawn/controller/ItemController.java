package com.shop.respawn.controller;


import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.ItemDto;
import com.shop.respawn.dto.OffsetPage;
import com.shop.respawn.dto.OffsetResponse;
import com.shop.respawn.service.ImageService;
import com.shop.respawn.service.ItemService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.shop.respawn.domain.ItemStatus.*;
import static com.shop.respawn.util.SessionUtil.getSellerIdFromSession;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final ImageService imageService;

    /**
     * 상품 등록
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerItem(
            @RequestPart("itemDto") ItemDto itemDto,
            @RequestPart("image") MultipartFile imageFile,
            HttpSession session
    ) {
        try {
            Long sellerId = getSellerIdFromSession(session);
            String imageUrl = imageService.saveImage(imageFile);
            itemDto.setImageUrl(imageUrl);
            Item created = itemService.registerItem(itemDto, sellerId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상품 등록 에러: " + e.getMessage());
        }
    }

    /**
     * Id 값으로 상품 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(@PathVariable String id) {
        Item item = itemService.getItemById(id);
        ItemDto itemDto = new ItemDto(item.getId(), item.getName(), item.getDescription(), item.getDeliveryType(), item.getDeliveryFee(), item.getCompany(),
                item.getCompanyNumber(), item.getPrice(), item.getStockQuantity(), item.getSellerId(), item.getImageUrl(), item.getCategoryIds(), item.getStatus());
        return ResponseEntity.ok(itemDto);
    }

    /**
     * 카테고리별 상품 조회
     */
    @GetMapping
    public ResponseEntity<OffsetResponse<ItemDto>> getItems(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "8") int limit
    ) {
        OffsetPage<Item> result = itemService.findItemsByOffset(category, offset, limit);

        List<ItemDto> itemDtos = result.items().stream()
                .map(item -> new ItemDto(item.getId(), item.getName(), item.getDescription(), item.getDeliveryType(), item.getDeliveryFee(), item.getCompany(),
                        item.getCompanyNumber(), item.getPrice(), item.getStockQuantity(), item.getSellerId(), item.getImageUrl(), item.getCategoryIds()))
                .toList();
        return ResponseEntity.ok(new OffsetResponse<>(itemDtos, offset, limit, result.total()));
    }

    /**
     * 자신이 등록한 아이템 조회
     */
    @GetMapping("/my-items")
    public ResponseEntity<List<ItemDto>> getItemsOfLoggedInSeller(HttpSession session) {
        Long sellerId = getSellerIdFromSession(session);  // 세션에서 로그인된 판매자 ID 조회

        List<Item> items = itemService.getItemsBySellerId(String.valueOf(sellerId));

        List<ItemDto> itemDtos = items.stream()
                .map(item -> new ItemDto(item.getId(),
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
                        item.getCategoryIds()))
                .toList();

        return ResponseEntity.ok(itemDtos);
    }

    /**
     * 상품 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable String id,
            @RequestPart("itemDto") ItemDto itemDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            HttpSession session) {
        try {
            Long sellerId = getSellerIdFromSession(session);

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = imageService.saveImage(imageFile);
                itemDto.setImageUrl(imageUrl);
            }

            Item updatedItem = itemService.updateItem(id, itemDto, sellerId);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상품 수정 에러: " + e.getMessage());
        }
    }

    /**
     * 상품 판매 일시중지
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseItem(@PathVariable String id, HttpSession session) {
        Long sellerId = getSellerIdFromSession(session);
        itemService.changeItemStatus(id, sellerId, PAUSED);
        return ResponseEntity.ok().body("상품이 일시중지되었습니다.");
    }

    /**
     * 상품 판매 중지
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopItem(@PathVariable String id, HttpSession session) {
        Long sellerId = getSellerIdFromSession(session);
        itemService.changeItemStatus(id, sellerId, STOPPED);
        return ResponseEntity.ok().body("상품 판매가 중지되었습니다.");
    }

    /**
     * 상품 판매 재개
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeItem(@PathVariable String id, HttpSession session) {
        Long sellerId = getSellerIdFromSession(session);
        itemService.changeItemStatus(id, sellerId, SALE);
        return ResponseEntity.ok().body("상품 판매가 재개되었습니다.");
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable String id, HttpSession session) {
        try {
            Long sellerId = getSellerIdFromSession(session);
            itemService.deleteItemIfNoPendingDelivery(id, sellerId);
            return ResponseEntity.ok(Map.of("message", "상품이 성공적으로 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 아이템 검색 (키워드)
     * 예: GET /api/items/search?query=아이폰
     */
    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> searchItems(@RequestParam(name = "query", required = false) String query) {
        List<Item> items = itemService.searchItems(query);
        List<ItemDto> itemDtos = items.stream()
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
                        item.getCategoryIds(),
                        item.getStatus()
                ))
                .toList();
        return ResponseEntity.ok(itemDtos);
    }

    /**
     * 아이템 검색 (키워드 + 카테고리 필터)
     * 예: GET /api/items/search/advanced?query=아이폰&categoryIds=phone&categoryIds=apple
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<List<ItemDto>> searchItemsAdvanced(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "categoryIds", required = false) List<String> categoryIds
    ) {
        List<Item> items = itemService.searchItemsByCategory(query, categoryIds);
        List<ItemDto> itemDtos = items.stream()
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
                        item.getCategoryIds(),
                        item.getStatus()
                ))
                .toList();
        return ResponseEntity.ok(itemDtos);
    }
}
