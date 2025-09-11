package com.shop.respawn.controller;

import com.shop.respawn.dto.point.ExpiringPointItemDto;
import com.shop.respawn.dto.point.ExpiringPointTotalDto;
import com.shop.respawn.dto.point.PointHistoryDto;
import com.shop.respawn.dto.point.PointLedgerDto;
import com.shop.respawn.service.LedgerPointService;
import com.shop.respawn.service.OrderService;
import com.shop.respawn.service.PointQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.shop.respawn.util.AuthenticationUtil.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final LedgerPointService ledgerPointService;
    private final PointQueryService pointQueryService;
    private final OrderService orderService;

    /**
     * 모든 포인트 조회
     */
    @GetMapping("/total")
    public ResponseEntity<?> getMyTotalPoints(Authentication authentication) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            long total = ledgerPointService.getTotal(buyerId);
            return ResponseEntity.ok(total);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if ("로그인이 필요합니다.".equals(message)) {
                return ResponseEntity.status(401).body(message);
            }
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 사용 가능한 모든 포인트 조회
     */
    @GetMapping("/total/active")
    public ResponseEntity<?> getMyActiveTotalPoints(Authentication authentication) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            ledgerPointService.expireBuyer(buyerId);
            long active = ledgerPointService.getActive(buyerId);
            return ResponseEntity.ok(active);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if ("로그인이 필요합니다.".equals(message)) {
                return ResponseEntity.status(401).body(message);
            }
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 임시 주문에 포인트 사용 적용 (DB에는 차감하지 않음)
     */
    @PostMapping("/apply")
    public ResponseEntity<String> applyPoints(
            Authentication authentication,
            @RequestParam Long orderId,
            @RequestParam Long usePointAmount
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            String result = orderService.applyPoints(buyerId, orderId, usePointAmount);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if ("권한이 없습니다.".equals(message)) {
                return ResponseEntity.status(403).body(message);
            } else if ("사용가능 포인트 부족".equals(message)) {
                return ResponseEntity.badRequest().body(message);
            } else if ("주문을 찾을 수 없습니다.".equals(message)) {
                return ResponseEntity.badRequest().body(message);
            }
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 포인트 적립 내역
     * 예시: /api/points/saves?page=0&size=20&sort=occurredAt,desc
     */
    @GetMapping("/saves")
    public Page<PointLedgerDto> getSaves(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return ledgerPointService.getSaves(buyerId, page, size, sort, year, month);
    }

    /**
     * 포인트 사용 내역
     * 예시: /api/points/uses?page=0&size=20&sort=occurredAt,desc
     */
    @GetMapping("/uses")
    public Page<PointLedgerDto> getUses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return ledgerPointService.getUses(buyerId, page, size, sort, year, month);
    }

    /**
     * 포인트 사용 취소
     */
    @PostMapping("/cancelUse")
    public ResponseEntity<String> cancelUse(
            @RequestParam("buyerId") Long buyerId,
            @RequestParam("useLedgerId") Long useLedgerId,
            @RequestParam("reason") String reason,
            @RequestParam("actor") String actor) {
        try {
            ledgerPointService.cancelUse(buyerId, useLedgerId, reason, actor);
            return ResponseEntity.ok("포인트 사용 취소가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    /**
     * 포인트 통합 내역
     * 예시: /api/points/history?page=0&size=20&sort=occurredAt,desc
     */
    @GetMapping("/history")
    public Page<PointHistoryDto> getAll(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return ledgerPointService.getAll(buyerId, page, size, sort, year, month);
    }

    /**
     * 이번 달 소멸 예정 합계(숫자만)
     */
    @GetMapping("/expire/this-month/total")
    public ExpiringPointTotalDto getThisMonthExpiringTotal(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return pointQueryService.getThisMonthExpiringTotal(buyerId);
    }

    /**
     * 이번 달 소멸 예정 목록(항목 리스트)
     */
    @GetMapping("/expire/this-month/list")
    public List<ExpiringPointItemDto> getThisMonthExpiringList(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return pointQueryService.getThisMonthExpiringList(buyerId);
    }

    /**
     * 특정 월의 소멸 예정 목록(항목 리스트)
     */
    @GetMapping("/expire/list")
    public List<ExpiringPointItemDto> getMonthlyExpiringList(
            Authentication authentication,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        return ledgerPointService.getMonthlyExpiringList(buyerId, year, month);
    }

}