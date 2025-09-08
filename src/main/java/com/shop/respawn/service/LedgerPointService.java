package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.point.ExpiringPointItemDto;
import com.shop.respawn.dto.point.PointHistoryDto;
import com.shop.respawn.dto.point.PointLedgerDto;
import com.shop.respawn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerPointService {

    private final BuyerRepository buyerRepository;
    private final PointLedgerRepository ledgerRepository;
    private final PointConsumeLinkRepository linkRepository;
    private final PointBalanceRepository balanceRepository;
    private final PointQueryService pointQueryService;

    // 내부 유틸: Balance 조회 또는 생성
    private PointBalance getOrCreateBalance(Long buyerId) {
        return balanceRepository.findById(buyerId)
                .orElseGet(() -> balanceRepository.save(PointBalance.init(buyerId)));
    }

    // 적립
    @Transactional
    public void savePoints(Long buyerId, long amount, LocalDateTime expiryAt,
                           Long refOrderId, String reason, String actor) {
        if (amount <= 0) throw new IllegalArgumentException("적립 금액은 0보다 커야 합니다.");
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다."));
        ledgerRepository.save(
                PointLedger.of(buyer, PointTransactionType.SAVE, amount,
                        LocalDateTime.now(), expiryAt, refOrderId, reason, actor)
        );
        PointBalance bal = getOrCreateBalance(buyerId);
        bal.addTotal(amount);
        bal.addActive(amount);
        balanceRepository.save(bal);
    }

    // FIFO 사용
    @Transactional
    public void usePoints(Long buyerId, long useAmount, Long refOrderId, String reason, String actor) {
        if (useAmount <= 0) throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        PointBalance bal = getOrCreateBalance(buyerId);
        if (bal.getActive() < useAmount) {
            throw new RuntimeException("사용 가능한 포인트가 부족합니다. (사용가능: " + bal.getActive() + ", 요청: " + useAmount + ")");
        }

        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다."));

        PointLedger use = ledgerRepository.save(
                PointLedger.of(buyer, PointTransactionType.USE, -useAmount,
                        LocalDateTime.now(), null, refOrderId, reason, actor)
        );

        long remain = useAmount;
        List<PointLedger> saves = ledgerRepository.findUsableSaveLedgers(buyerId, LocalDateTime.now());
        for (PointLedger s : saves) {
            if (remain <= 0) break;

            long consumed = getConsumed(s);
            long availableFromThisSave = s.getAmount() - consumed;
            if (availableFromThisSave <= 0) continue;

            long toConsume = Math.min(availableFromThisSave, remain);

            linkRepository.save(PointConsumeLink.of(s, use, toConsume));
            remain -= toConsume;
        }
        if (remain > 0) {
            // 이 경우는 집계와 원장의 불일치. 트랜잭션 롤백 유도.
            throw new IllegalStateException("포인트 사용 처리 중 일관성 오류");
        }

        bal.addTotal(-useAmount);
        bal.addActive(-useAmount);
        bal.addUsed(useAmount);
        balanceRepository.save(bal);
    }

    // 사용 취소
    @Transactional
    public void cancelUse(Long buyerId, Long useLedgerId, String reason, String actor) {
        PointLedger use = ledgerRepository.findById(useLedgerId)
                .orElseThrow(() -> new RuntimeException("USE 레코드를 찾을 수 없습니다."));
        if (use.getType() != PointTransactionType.USE) {
            throw new IllegalArgumentException("USE 레코드가 아닙니다.");
        }
        long usedAbs = Math.abs(use.getAmount());

        Buyer buyer = use.getBuyer();
        PointLedger cancelUse = ledgerRepository.save(
                PointLedger.of(buyer, PointTransactionType.CANCEL_USE, usedAbs,
                        LocalDateTime.now(), null, use.getRefOrderId(), reason, actor)
        );

        // 링크 되돌리기(최근 링크부터 되돌리는 정책 가능. 여기선 단순 전체 되돌림)
        List<PointConsumeLink> links = linkRepository.findByUseLedger(use);
        for (PointConsumeLink link : links) {
            // 소비를 되돌릴 땐 별도 링크가 필요한가?
            // 감사 추적용으로 CANCEL_USE도 SAVE와의 링크를 남길 수 있지만, 단순화해 생략 가능.
            // 필요 시: linkRepository.save(PointConsumeLink.of(link.getSaveLedger(), cancelUse, link.getConsumedAmount()));
        }
        // 집계 되돌림
        PointBalance bal = getOrCreateBalance(buyer.getId());
        bal.addTotal(usedAbs);
        bal.addActive(usedAbs);
        bal.addUsed(-usedAbs);
        balanceRepository.save(bal);
    }

    // 만료 배치
    @Transactional
    public long expireBuyer(Long buyerId) {
        List<PointLedger> candidates = ledgerRepository.findExpireCandidates(buyerId, LocalDateTime.now());
        if (candidates.isEmpty()) return 0;

        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다."));

        long totalExpired = 0;
        for (PointLedger save : candidates) {
            long consumed = getConsumed(save);
            long remain = save.getAmount() - consumed;
            if (remain <= 0) continue;

            PointLedger expire = ledgerRepository.save(
                    PointLedger.of(buyer, PointTransactionType.EXPIRE, -remain,
                            LocalDateTime.now(), null, null, "만료", "system")
            );
            linkRepository.save(PointConsumeLink.of(save, expire, remain));
            totalExpired += remain;
        }
        if (totalExpired > 0) {
            PointBalance bal = getOrCreateBalance(buyerId);
            bal.addTotal(-totalExpired);
            bal.addActive(-totalExpired);
            bal.addExpired(totalExpired);
            balanceRepository.save(bal);
        }
        return totalExpired;
    }

    // 적립 취소(예: 결제 취소 정책에 따라)
    @Transactional
    public void cancelSave(Long buyerId, Long saveLedgerId, String reason, String actor) {
        PointLedger save = ledgerRepository.findById(saveLedgerId)
                .orElseThrow(() -> new RuntimeException("SAVE 레코드를 찾을 수 없습니다."));
        if (save.getType() != PointTransactionType.SAVE) {
            throw new IllegalArgumentException("SAVE 레코드가 아닙니다.");
        }
        long consumed = getConsumed(save);
        long remain = save.getAmount() - consumed;
        if (remain <= 0) {
            // 이미 전부 사용됨: 정책적으로 부채 처리/불가 처리 등 결정 필요
            throw new RuntimeException("이미 전부 사용되어 적립 취소가 불가합니다.");
        }

        Buyer buyer = save.getBuyer();
        PointLedger cancelSave = ledgerRepository.save(
                PointLedger.of(buyer, PointTransactionType.CANCEL_SAVE, -remain,
                        LocalDateTime.now(), null, save.getRefOrderId(), reason, actor)
        );
        // 필요시 링크 생성으로 추적 가능: linkRepository.save(PointConsumeLink.of(save, cancelSave, remain));

        PointBalance bal = getOrCreateBalance(buyer.getId());
        bal.addTotal(-remain);
        bal.addActive(-remain);
        balanceRepository.save(bal);
    }

    // SAVE에 대해 현재까지 소비된 양
    private long getConsumed(PointLedger save) {
        return linkRepository.findBySaveLedger(save).stream()
                .mapToLong(PointConsumeLink::getConsumedAmount)
                .sum();
    }

    public Page<PointLedgerDto> getSaves(Long buyerId, int page, int size, String sort, Integer year, Integer month) {
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getSavesByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getSaves(buyerId, pageable);
    }

    public Page<PointLedgerDto> getUses(Long buyerId, int page, int size, String sort, Integer year, Integer month) {
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getUsesByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getUses(buyerId, pageable);
    }

    public Page<PointHistoryDto> getAll(Long buyerId, int page, int size, String sort, Integer year, Integer month) {
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getAllByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getAll(buyerId, pageable);
    }

    public List<ExpiringPointItemDto> getMonthlyExpiringList(Long buyerId, int year, int month) {
        LocalDateTime[] range = toMonthRange(year, month);
        return pointQueryService.getMonthlyExpiringList(buyerId, range[0], range[1]);
    }

    // 유틸: "occurredAt,desc" → Sort
    private Sort toSort(String sortParam) {
        // "occurredAt,desc" 또는 "amount,asc" 형태를 1개 받아 처리
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.desc("occurredAt")).and(Sort.by(Sort.Order.desc("id")));
        }
        String[] parts = sortParam.split(",");
        String prop = parts[0];
        String dir = (parts.length > 1) ? parts[1] : "desc";
        Sort.Order order = "asc".equalsIgnoreCase(dir) ? Sort.Order.asc(prop) : Sort.Order.desc(prop);
        // 기본 tie-break
        return Sort.by(order).and(Sort.by(Sort.Order.desc("id")));
    }

    private static LocalDateTime[] toMonthRange(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        return new LocalDateTime[]{ first.atStartOfDay(), last.atTime(LocalTime.MAX) };
    }

    @Transactional(readOnly = true)
    public long getActive(Long buyerId) {
        return getOrCreateBalance(buyerId).getActive();
    }

    @Transactional(readOnly = true)
    public long getTotal(Long buyerId) {
        return getOrCreateBalance(buyerId).getTotal();
    }
}
