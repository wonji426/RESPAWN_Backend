package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.PointLedger;
import com.shop.respawn.domain.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PointLedgerRepositoryCustom {
    List<PointLedger> findUsableSaveLedgers(Long buyerId, LocalDateTime now);

    List<PointLedger> findExpireCandidates(Long buyerId, LocalDateTime now);

    Page<PointLedger> findByBuyerAndTypes(Long buyerId, Iterable<PointTransactionType> types, Pageable pageable);

    // 통합(모든 타입) 목록
    Page<PointLedger> findAllByBuyer(Long buyerId, Pageable pageable);

    // 월별(occurredAt between) 조회
    Page<PointLedger> findByBuyerAndTypesAndOccurredBetween(Long buyerId, Iterable<PointTransactionType> types,
                                                            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<PointLedger> findAllByBuyerAndOccurredBetween(Long buyerId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 이번 달 범위 내(expiryAt between monthStart~monthEnd) 만료 예정 SAVE(잔여>0)
    List<PointLedger> findMonthlyExpireCandidates(Long buyerId, LocalDateTime monthStart, LocalDateTime monthEnd);

    // 특정 SAVE 레코드의 소비 합계
    Long sumConsumedAmountOfSave(PointLedger saveLedger);
}
