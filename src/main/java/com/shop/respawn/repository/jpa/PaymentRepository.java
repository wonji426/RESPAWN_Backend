package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {

    Optional<Payment> findByOrder(Order order);

}
