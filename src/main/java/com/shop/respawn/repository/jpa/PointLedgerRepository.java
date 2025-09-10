package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.PointLedger;
import com.shop.respawn.domain.PointTransactionType;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long>, PointLedgerRepositoryCustom {

    Optional<PointLedger> findTopByBuyer_IdAndTypeAndRefOrderIdOrderByOccurredAtDesc(
            Long buyerId,
            PointTransactionType type,
            Long refOrderId
    );
}
