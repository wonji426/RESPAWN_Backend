package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * 특정 구매자가 찜한 특정 상품을 조회합니다. (찜 토글 로직에서 사용)
     */
    Optional<Wishlist> findByBuyerIdAndItemId(Long buyerId, String itemId);

    /**
     * 특정 구매자가 찜한 모든 상품을 최신순(찜한 날짜 역순)으로 페이징하여 조회합니다.
     */
    Page<Wishlist> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /**
     * 특정 구매자가 특정 상품을 찜했는지 여부를 확인합니다. (상품 상세 페이지에서 사용)
     */
    boolean existsByBuyerIdAndItemId(Long buyerId, String itemId);

}
