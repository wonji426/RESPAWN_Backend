package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepositoryCustom<T extends User> extends JpaRepository<T, Long> {

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
