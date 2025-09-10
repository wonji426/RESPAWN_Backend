package com.shop.respawn.repository.jpa;

import java.time.LocalDateTime;

public interface PaymentRepositoryCustom {

    Long sumMonthlyAmountByBuyer(Long buyerId, LocalDateTime start, LocalDateTime end);
}
