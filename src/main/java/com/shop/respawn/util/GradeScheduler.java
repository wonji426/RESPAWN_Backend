package com.shop.respawn.util;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.repository.BuyerRepository;
import com.shop.respawn.service.UserGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GradeScheduler {

    private final BuyerRepository buyerRepository;
    private final UserGradeService userGradeService;

    // 매일 새벽 00시 00분
    @Scheduled(cron = "0 0 0 1 * *")
    public void recalcAllBuyerTiers() {
        // 규모가 크면 페이징 처리 권장
        List<Long> buyerIds = buyerRepository.findAll().stream()
                .map(Buyer::getId)
                .toList();
        userGradeService.recalcMany(buyerIds);
    }
}