package com.shop.respawn.service;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Cart;
import com.shop.respawn.domain.CartItem;
import com.shop.respawn.domain.Item;
import com.shop.respawn.repository.BuyerRepository;
import com.shop.respawn.repository.CartRepository;
import com.shop.respawn.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final BuyerRepository buyerRepository;
    private final ItemRepository itemRepository;

    /**
     * 장바구니에 상품 추가
     */
    public void addItemToCart(Long buyerId, String itemId, Long count) {
        // 구매자 조회
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다: " + buyerId));

        // 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + itemId));

        // 재고 확인
        if (item.getStockQuantity() < count) {
            throw new RuntimeException("재고가 부족합니다. 현재 재고: " + item.getStockQuantity());
        }

        // 구매자의 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setBuyer(buyer);
                    return cartRepository.save(newCart);
                });

        // 이미 장바구니에 있는 상품인지 확인
        boolean itemExists = false;
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getItemId().equals(itemId)) {
                // 기존 수량에 추가
                cartItem.increaseQuantity(count);
                itemExists = true;
                break;
            }
        }

        // 새로운 상품이면 장바구니에 추가
        if (!itemExists) {
            CartItem cartItem = CartItem.createCartItem(cart, itemId, item.getPrice(), count);
            cart.addCartItem(cartItem);
        }

        cartRepository.save(cart);
    }

    /**
     * 장바구니 조회
     */
    public Cart getCartByBuyerId(Long buyerId) {
        return cartRepository.findByBuyerId(buyerId)
                .orElse(null);
    }

    /**
     * 장바구니 아이템 수량 증가
     */
    public void increaseCartItemQuantity(Long buyerId, Long cartItemId, Long amount) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니 아이템을 찾을 수 없습니다"));

        cartItem.increaseQuantity(amount);
        cartRepository.save(cart);
    }

    /**
     * 장바구니 아이템 수량 감소
     */
    public void decreaseCartItemQuantity(Long buyerId, Long cartItemId, Long amount) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("장바구니 아이템을 찾을 수 없습니다"));

        cartItem.decreaseQuantity(amount);
        cartRepository.save(cart);
    }

    /**
     * 장바구니에서 아이템 제거
     */
    public void removeCartItem(Long buyerId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }

        int deletedCount = cartRepository.deleteByIdsAndBuyerId(buyerId, cartItemIds);

        if (deletedCount == 0) {
            throw new EntityNotFoundException("삭제할 장바구니 아이템을 찾을 수 없거나, 소유자가 일치하지 않습니다.");
        }
    }

    /**
     * 장바구니 전체 금액 계산
     */
    @Transactional(readOnly = true)
    public Long calculateTotalPrice(Long buyerId) {
        Cart cart = getCartByBuyerId(buyerId);
        if (cart == null) return 0L; // Long 리터럴

        return cart.getCartItems().stream()
                .mapToLong(CartItem::getTotalPrice) // long 타입 스트림으로 변환
                .sum();
    }

    /**
     * 장바구니 비우기 (아이템만 제거, 장바구니는 유지)
     */
    public void clearCart(Long buyerId) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        cart.getCartItems().clear();
        cartRepository.save(cart);
    }
}
