package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long>, AdminRepositoryCustom {
    Admin findByUsername(String username);
}
