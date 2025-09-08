package com.shop.respawn.controller;

import com.shop.respawn.dto.gradeRecalc.UserGradeResponse;
import com.shop.respawn.dto.gradeRecalc.GradeRecalcRequest;
import com.shop.respawn.dto.gradeRecalc.GradeRecalcResponse;
import com.shop.respawn.service.UserGradeService;
import com.shop.respawn.util.SessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.shop.respawn.util.SessionUtil.getUserIdFromAuthentication;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grade")
public class UserGradeController {

    private final UserGradeService userGradeService;

    // 1) 내 멤버십 등급 조회 (로그인 필요)
    @GetMapping("/my-grade")
    public ResponseEntity<UserGradeResponse> myGrade(Authentication authentication) {
        // session에서 buyerId 조회 후 buyer의 membershipTier 조회
        Long buyerId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(userGradeService.findBuyerGradeById(buyerId));
    }

    // 2) 특정 구매자 등급 조회 (관리자/운영자용)
    @GetMapping("/{buyerId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<UserGradeResponse> gradeByBuyerId(@PathVariable Long buyerId) {
        return ResponseEntity.ok(userGradeService.findBuyerGradeById(buyerId));
    }

    // 1) 단일 사용자 강제 갱신
    @PostMapping("/recalc/{buyerId}")
    @Secured("ROLE_ADMIN")
    public GradeRecalcResponse recalcOne(
            @PathVariable Long buyerId
    ) {
        userGradeService.recalcBuyerGrade(buyerId);
        return new GradeRecalcResponse(1, 1, 0, "buyerId=" + buyerId + " 등급 갱신 완료");
    }

    // 2) 다수/전체 강제 갱신
    @PostMapping("/recalc")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<GradeRecalcResponse> recalcMany(
            @RequestBody(required = false)
            GradeRecalcRequest request
    ) {
        long start = System.currentTimeMillis();
        List<Long> buyerIds = (request == null || request.getBuyerIds() == null || request.getBuyerIds().isEmpty())
                ? null   // null이면 서비스에서 전체 처리
                : request.getBuyerIds();

        GradeRecalcResponse result = userGradeService.recalcMany(buyerIds);

        log.info("POST /grades/recalc - processed={}, succeeded={}, failed={}, elapsedMs={}",
                result.getProcessed(), result.getSucceeded(), result.getFailed(),
                System.currentTimeMillis() - start);

        return ResponseEntity.ok(result);
    }
}