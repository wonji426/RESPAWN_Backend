package com.shop.respawn.controller;

import com.shop.respawn.dto.PageResponse;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.shop.respawn.util.AuthenticationUtil.getUserIdFromAuthentication;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 내 찜 목록 조회
     */
    @GetMapping
    public ResponseEntity<PageResponse<ItemDto>> getMyWishlist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);

            Page<ItemDto> myWishlist = wishlistService.getMyWishlist(buyerId, pageable);

            return ResponseEntity.ok(PageResponse.from(myWishlist));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 찜하기 / 찜 취소 토글
     */
    @PostMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            Authentication authentication,
            @PathVariable String itemId
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            boolean isAdded = wishlistService.toggleWishlist(buyerId, itemId);

            String message = isAdded ? "찜 목록에 추가되었습니다." : "찜 목록에서 삭제되었습니다.";

            return ResponseEntity.ok(Map.of(
                    "isWished", isAdded,
                    "message", message
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}