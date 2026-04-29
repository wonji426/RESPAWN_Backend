package com.shop.respawn.controller;

import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.dto.PageResponse;
import com.shop.respawn.dto.item.ItemSummaryDto;
import com.shop.respawn.service.ImageService;
import com.shop.respawn.service.ItemService;
import com.shop.respawn.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.shop.respawn.domain.ItemStatus.*;
import static com.shop.respawn.util.AuthenticationUtil.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final ImageService imageService;
    private final WishlistService wishlistService;

    /**
     * 상품 등록
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerItem(
            Authentication authentication,
            @RequestPart("itemDto") ItemDto itemDto,
            @RequestPart("image") MultipartFile imageFile
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
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
    public ResponseEntity<ItemDto> getItem(
            @PathVariable String id,
            Authentication authentication
    ) {
        ItemDto itemDto = itemService.findItemWithCategoryName(id);

        boolean isWished = false;

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            try {
                Long buyerId = getUserIdFromAuthentication(authentication);
                // 💡 여기서 방금 만든 서비스 메서드를 호출합니다!
                isWished = wishlistService.checkIsWished(buyerId, id);
            } catch (Exception e) {
                // 토큰 파싱 에러나 판매자 계정일 경우 등을 대비해 안전하게 잡고 false 처리
            }
        }

        // 3. DTO에 찜 여부 세팅
        itemDto.setWished(isWished);

        return ResponseEntity.ok(itemDto);
    }

    /**
     * 카테고리별 상품 조회
     */
    @GetMapping
    public ResponseEntity<PageResponse<ItemDto>> getItems(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ItemDto> items = itemService.getItemByCategory(category, pageable);
        return ResponseEntity.ok(PageResponse.from(items));
    }

    /**
     * 자신이 등록한 아이템 조회
     */
    @GetMapping("/my-items")
    public ResponseEntity<PageResponse<ItemDto>> getItemsOfLoggedInSeller(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir
    ) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        Sort.Direction direction = Sort.Direction.fromString(dir);
        Sort sortOrder = Sort.by(direction, sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ItemDto> items = itemService.getSimpleItemsBySellerId(String.valueOf(sellerId),search, pageable);
        return ResponseEntity.ok(PageResponse.from(items));
    }

    @GetMapping("/my-items/summary")
    public ResponseEntity<List<ItemSummaryDto>> getMyItemNames(Authentication authentication) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        List<ItemSummaryDto> result = itemService.getMyItemIdAndNames(String.valueOf(sellerId));
        return ResponseEntity.ok(result);
    }

    /**
     * 상품 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            Authentication authentication,
            @PathVariable String id,
            @RequestPart("itemDto") ItemDto itemDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = imageService.saveImage(imageFile);
                itemDto.setImageUrl(imageUrl);
            }

            Item updatedItem = itemService.updateItem(id, itemDto, String.valueOf(sellerId));
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상품 수정 에러: " + e.getMessage());
        }
    }

    /**
     * 상품 판매 일시중지
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseItem(
            Authentication authentication,
            @PathVariable String id
    ) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        itemService.changeItemStatus(id, String.valueOf(sellerId), PAUSED);
        return ResponseEntity.ok().body("상품이 일시중지되었습니다.");
    }

    /**
     * 상품 판매 중지
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopItem(
            Authentication authentication,
            @PathVariable String id
    ) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        itemService.changeItemStatus(id, String.valueOf(sellerId), STOPPED);
        return ResponseEntity.ok().body("상품 판매가 중지되었습니다.");
    }

    /**
     * 상품 판매 재개
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeItem(
            Authentication authentication,
            @PathVariable String id
    ) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        itemService.changeItemStatus(id, String.valueOf(sellerId), SALE);
        return ResponseEntity.ok().body("상품 판매가 재개되었습니다.");
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(
            Authentication authentication,
            @PathVariable String id
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            itemService.deleteItemIfNoPendingDelivery(id, String.valueOf(sellerId));
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
                        item.getCategory(),
                        item.getStatus()
                ))
                .toList();
        return ResponseEntity.ok(itemDtos);
    }

    /**
     * 아이템 검색 페이징 (키워드 + 카테고리 + 가격 범위 + 배송방식필터)
     * 예: GET /api/items/search/advanced?query=아이폰&categoryIds=phone&categoryIds=apple
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<PageResponse<ItemDto>> searchItemsAdvanced(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "categoryIds", required = false) List<String> categoryIds,
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "minPrice", required = false) Long minPrice,
            @RequestParam(name = "maxPrice", required = false) Long maxPrice,
            @RequestParam(name = "deliveryType", required = false) String deliveryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort, // 정렬 필드
            @RequestParam(defaultValue = "desc") String dir  // 정렬 방향
    ) {
        Sort.Direction direction = Sort.Direction.fromString(dir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<ItemDto> resultPage = itemService.searchItemsByCategory(
                query, categoryIds, company, minPrice, maxPrice, deliveryType, pageable
        );

        return ResponseEntity.ok(PageResponse.from(resultPage));
    }
}
