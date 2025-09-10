package com.shop.respawn.service;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.dto.gradeRecalc.UserGradeResponse;
import com.shop.respawn.dto.gradeRecalc.GradeRecalcResponse;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.util.GradePolicy;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.PaymentRepository;
import com.shop.respawn.util.MonthlyPeriodUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserGradeService {

    private final BuyerRepository buyerRepository;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final GradePolicy gradePolicy;

    // 저번달 기준으로 재계산
    public void recalcBuyerGrade(Long buyerId) {
        LocalDateTime[] range = MonthlyPeriodUtil.previousMonthRange();
        LocalDateTime start = range[0];
        LocalDateTime end   = range[1];

        Long monthlyAmount = paymentRepository.sumMonthlyAmountByBuyer(buyerId, start, end);
        Grade newGrade = gradePolicy.resolveGrade(monthlyAmount);
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 없음: " + buyerId));
        Grade oldGrade = buyer.getGrade();
        if (oldGrade != newGrade) {
            buyer.updateGrade(newGrade);
            // 등급 변경 시 쿠폰 발급
            couponService.issueGradeCoupon(buyerId, newGrade);
        }
        // 변경감지로 flush
    }

    /**
     * 유저 등급 조회
     */
    public UserGradeResponse findBuyerGradeById(Long buyerId) {
        UserQueryDto userQueryDto = buyerRepository.findUserGradeById(buyerId);
        return new UserGradeResponse(buyerId, userQueryDto.getUsername(), userQueryDto.getGrade());
    }

    /**
     * buyerIds가 null 또는 empty면 전체 대상 처리.
     * 각 buyerId는 개별 트랜잭션으로 처리하여 부분 성공을 허용.
     */
    public GradeRecalcResponse recalcMany(List<Long> buyerIds) {
        List<Long> targets = resolveTargets(buyerIds);

        int processed = 0, succeeded = 0, failed = 0;

        for (Long id : targets) {
            processed++;
            try {
                // 개별 트랜잭션 경계에서 실행
                recalcOneInNewTx(id);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("등급 갱신 실패 buyerId={}", id, e);
            }
        }

        String msg = "processed=" + processed + ", succeeded=" + succeeded + ", failed=" + failed;
        return new GradeRecalcResponse(processed, succeeded, failed, msg);
    }

    private List<Long> resolveTargets(List<Long> buyerIds) {
        if (buyerIds == null || buyerIds.isEmpty()) {
            return buyerRepository.findAll().stream()
                    .map(Buyer::getId)
                    .toList();
        }
        return buyerIds;
    }

    // REQUIRES_NEW는 “항상 기존 트랜잭션을 멈추고, 이 메서드를 별도의 새 트랜잭션으로 실행”하여,
    // 내부 작업을 독립적으로 커밋/롤백하게 하는 전파 옵션입니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void recalcOneInNewTx(Long buyerId) {
        recalcBuyerGrade(buyerId);
    }
}
