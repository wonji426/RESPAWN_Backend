package com.shop.respawn.repository;

import java.time.LocalDateTime;

public interface PaymentRepositoryCustom {

    Long sumMonthlyAmountByBuyer(Long buyerId, LocalDateTime start, LocalDateTime end);
}
