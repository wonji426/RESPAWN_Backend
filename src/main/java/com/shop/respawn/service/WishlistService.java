package com.shop.respawn.service;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.Wishlist;
import com.shop.respawn.dto.item.ItemDto;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WishlistService {

    // 필요한 의존성 주입 (Repository 및 다른 Service)
    private final WishlistRepository wishlistRepository;
    private final BuyerRepository buyerRepository;
    private final ItemService itemService; // MongoDB 데이터를 가져오기 위해 주입

    /**
     * 내가 찜한 목록 조회 (여기에 작성해 주신 코드를 넣습니다!)
     */
    @Transactional(readOnly = true)
    public Page<ItemDto> getMyWishlist(Long buyerId, Pageable pageable) {
        // 1. MySQL에서 내가 찜한 목록 조회
        Page<Wishlist> wishlistPage = wishlistRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);

        if (wishlistPage.isEmpty()) return Page.empty(pageable);

        // 2. 찜한 itemId들만 추출
        List<String> itemIds = wishlistPage.stream()
                .map(Wishlist::getItemId)
                .toList();

        // 3. MongoDB에서 Item 정보 일괄 조회
        List<Item> items = itemService.getPartialItemsByIds(itemIds);
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

        // 4. DTO 변환 반환
        List<ItemDto> dtos = wishlistPage.stream()
                .map(w -> {
                    Item item = itemMap.get(w.getItemId());
                    // Item 엔티티를 ItemDto로 변환 (ItemDto 생성자에 맞게 수정 필요)
                    return new ItemDto(
                            item.getId(),
                            item.getName(),
                            item.getCompany(),
                            item.getPrice(),
                            item.getImageUrl()
                    );
                }).toList();

        return new PageImpl<>(dtos, pageable, wishlistPage.getTotalElements());
    }

    /**
     * 찜하기 / 찜 취소 토글 메서드 (이전에 설명해 드린 로직)
     */
    public boolean toggleWishlist(Long buyerId, String itemId) {
        Optional<Wishlist> optionalWishlist = wishlistRepository.findByBuyerIdAndItemId(buyerId, itemId);

        Item item = itemService.getItemById(itemId);

        if (optionalWishlist.isPresent()) {
            wishlistRepository.delete(optionalWishlist.get());

            item.removeWishCount();
            itemService.save(item);

            return false; // 찜 해제
        } else {
            Buyer buyer = buyerRepository.findById(buyerId).orElseThrow();
            Wishlist wishlist = new Wishlist();
            wishlist.setBuyer(buyer);
            wishlist.setItemId(itemId);
            wishlistRepository.save(wishlist);

            item.addWishCount();
            itemService.save(item);

            return true; // 찜 추가
        }
    }

    @Transactional(readOnly = true)
    public boolean checkIsWished(Long buyerId, String itemId) {
        if (buyerId == null || itemId == null) {
            return false;
        }
        return wishlistRepository.existsByBuyerIdAndItemId(buyerId, itemId);
    }

    /**
     * 내가 찜한 상품의 총 갯수를 조회합니다. (마이페이지 요약용)
     */
    @Transactional(readOnly = true)
    public long countMyWishlist(Long buyerId) {
        if (buyerId == null) {
            return 0;
        }
        return wishlistRepository.countByBuyerId(buyerId);
    }
}
