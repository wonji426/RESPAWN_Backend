package com.shop.respawn.repository;

import com.shop.respawn.domain.Cart;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByBuyerId(Long buyerId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id IN :ids AND ci.cart.buyer.id = :buyerId")
    int deleteByIdsAndBuyerId(@Param("buyerId") Long buyerId, @Param("ids") List<Long> ids);
}
