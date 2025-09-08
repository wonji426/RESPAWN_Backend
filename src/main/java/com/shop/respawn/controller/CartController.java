package com.shop.respawn.controller;

import com.shop.respawn.domain.Cart;
import com.shop.respawn.domain.Item;
import com.shop.respawn.dto.CartItemDto;
import com.shop.respawn.dto.CartItemIdsRequest;
import com.shop.respawn.dto.QuantityChangeRequest;
import com.shop.respawn.service.CartService;
import com.shop.respawn.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.shop.respawn.util.SessionUtil.getUserIdFromAuthentication;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    /**
     * 장바구니에 상품 추가
     */
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            Authentication authentication,
            @RequestBody CartItemDto cartItemDto
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        try {
            cartService.addItemToCart(buyerId, cartItemDto.getItemId(), cartItemDto.getCount());
            return ResponseEntity.ok("장바구니에 상품이 추가되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 장바구니 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);

        Cart cart = cartService.getCartByBuyerId(buyerId);
        if (cart == null) {
            return ResponseEntity.ok(Map.of(
                    "cartItems", List.of(),
                    "totalPrice", 0
            ));
        }

        // 장바구니 아이템 정보와 상품 정보를 합쳐서 반환
        List<Map<String, Object>> cartItemsWithDetails = cart.getCartItems().stream()
                .map(cartItem -> {
                    Item item = itemService.getItemById(cartItem.getItemId());
                    Map<String, Object> itemDetail = new HashMap<>();
                    itemDetail.put("cartItemId", cartItem.getId());
                    itemDetail.put("itemId", cartItem.getItemId());
                    itemDetail.put("itemName", item.getName());
                    itemDetail.put("itemDescription", item.getDescription());
                    itemDetail.put("itemPrice", item.getPrice());
                    itemDetail.put("cartPrice", cartItem.getCartPrice());
                    itemDetail.put("count", cartItem.getCount());
                    itemDetail.put("totalPrice", cartItem.getTotalPrice());
                    itemDetail.put("imageUrl", item.getImageUrl());
                    itemDetail.put("stockQuantity", item.getStockQuantity());
                    return itemDetail;
                })
                .collect(Collectors.toList());

        Long totalPrice = cartService.calculateTotalPrice(buyerId);

        Map<String, Object> response = new HashMap<>();
        response.put("cartItems", cartItemsWithDetails);
        response.put("totalPrice", totalPrice);
        response.put("itemCount", cartItemsWithDetails.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/{cartItemId}/increase")
    public ResponseEntity<String> increaseCartItemQuantity(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @RequestBody @Valid QuantityChangeRequest request) {

        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            cartService.increaseCartItemQuantity(buyerId, cartItemId, request.getAmount());
            return ResponseEntity.ok("장바구니 아이템 수량이 증가되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 요청입니다: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
        }
    }

    @PostMapping("/items/{cartItemId}/decrease")
    public ResponseEntity<String> decreaseCartItemQuantity(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @RequestBody @Valid QuantityChangeRequest request
    ) {

        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            cartService.decreaseCartItemQuantity(buyerId, cartItemId, request.getAmount());
            return ResponseEntity.ok("장바구니 아이템 수량이 감소되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 요청입니다: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
        }
    }

    /**
     * 장바구니에서 선택 상품 제거
     */
    @DeleteMapping("/items/delete")
    public ResponseEntity<String> removeFromCart(
            Authentication authentication,
            @RequestBody CartItemIdsRequest request
    ) {

        Long buyerId = getUserIdFromAuthentication(authentication);
        List<Long> ids = request.getCartItemIds();

        try {
            cartService.removeCartItem(buyerId, ids);
            return ResponseEntity.ok("상품이 장바구니에서 제거되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 장바구니에서 상품 제거
     */
    @DeleteMapping
    public ResponseEntity<String> clearCart(Authentication authentication) {

        Long buyerId = getUserIdFromAuthentication(authentication);

        cartService.clearCart(buyerId);

        return ResponseEntity.ok("장바구니를 모두 비웠습니다.");
    }
}
