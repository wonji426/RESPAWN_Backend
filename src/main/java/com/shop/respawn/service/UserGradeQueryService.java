package com.shop.respawn.service;

import com.shop.respawn.util.GradePolicy;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Grade;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.PaymentRepository;
import com.shop.respawn.util.MonthlyPeriodUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserGradeQueryService {

    private final BuyerRepository buyerRepository;
    private final PaymentRepository paymentRepository;
    private final GradePolicy gradePolicy;

    public Result getPrevMonthTier(Long buyerId) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 없음: " + buyerId));

        LocalDateTime[] range = MonthlyPeriodUtil.previousMonthRange();
        Long amount = paymentRepository.sumMonthlyAmountByBuyer(buyerId, range[0], range[1]);
        Grade tier = gradePolicy.resolveGrade(amount);
        return new Result(buyer, amount, range[0], range[1], tier);
    }

    public record Result(Buyer buyer, Long amount, LocalDateTime start, LocalDateTime end, Grade tier) {}
}