package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Seller;
import com.shop.respawn.repository.UserRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface SellerRepository extends UserRepositoryCustom<Seller>, SellerRepositoryCustom {

    Seller findByUsername(String username);

    Boolean existsByUsername(String username);

    Seller findByNameAndEmail(String name, String email);

    Seller findByNameAndPhoneNumber(String name, String phoneNumber);

    Seller findByUsernameAndNameAndEmail(String username, String name, String email);

    Seller findByUsernameAndNameAndPhoneNumber(String username, String name, String phoneNumber);

    Page<Seller> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Seller> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<Seller> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Seller> findByPhoneNumberContaining(String phoneNumber, Pageable pageable);

    Page<Seller> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Seller> findByNameContainingIgnoreCaseAndCreatedAtBetween(String keyword, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Seller> findByUsernameContainingIgnoreCaseAndCreatedAtBetween(String keyword, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Seller> findByEmailContainingIgnoreCaseAndCreatedAtBetween(String keyword, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Seller> findByPhoneNumberContainingAndCreatedAtBetween(String keyword, LocalDateTime start, LocalDateTime end, Pageable pageable);

}
