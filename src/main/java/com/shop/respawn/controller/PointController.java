package com.shop.respawn.controller;

import com.shop.respawn.domain.Order;
import com.shop.respawn.dto.point.ExpiringPointItemDto;
import com.shop.respawn.dto.point.ExpiringPointTotalDto;
import com.shop.respawn.dto.point.PointHistoryDto;
import com.shop.respawn.dto.point.PointLedgerDto;
import com.shop.respawn.repository.OrderRepository;
import com.shop.respawn.service.LedgerPointService;
import com.shop.respawn.service.PointQueryService;
import com.shop.respawn.util.RedisUtil;
import com.shop.respawn.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final LedgerPointService ledgerPointService;
    private final PointQueryService pointQueryService;
    private final OrderRepository orderRepository;
    private final RedisUtil redisUtil;

    @GetMapping("/total")
    public long getMyTotalPointsV2(HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        return ledgerPointService.getTotal(buyerId);
    }

    @GetMapping("/total/active")
    public long getMyActiveTotalPointsV2(HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        return ledgerPointService.getActive(buyerId);
    }

    /**
     * 임시 주문에 포인트 사용 적용 (DB에는 차감하지 않음)
     */
    @PostMapping("/apply")
    public ResponseEntity<String> applyPoints(
            @RequestParam Long orderId,
            @RequestParam Long usePointAmount,
            HttpSession session) {

        Long buyerId = SessionUtil.getBuyerIdFromSession(session);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (!order.getBuyer().getId().equals(buyerId)) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        long activePoints = ledgerPointService.getActive(buyerId);
        if (usePointAmount > activePoints) {
            return ResponseEntity.badRequest().body("사용가능 포인트 부족");
        }

        // 포인트 적용을 N번 누를 시
        String redisKey = "order:" + orderId + ":pointAmount";
        if(order.getUsedPointAmount() != 0L){
            String data = redisUtil.getData(redisKey);
            order.setTotalAmount(order.getTotalAmount() + Long.parseLong(data));
        }

        order.setTotalAmount(order.getTotalAmount() - usePointAmount);
        redisUtil.setData(redisKey, String.valueOf(usePointAmount));
        order.setUsedPointAmount(usePointAmount);

        orderRepository.save(order);

        return ResponseEntity.ok("포인트 " + usePointAmount + "원이 적용되었습니다.");
    }

    // 적립 내역: /api/points/saves?page=0&size=20&sort=occurredAt,desc
    @GetMapping("/saves")
    public Page<PointLedgerDto> getSaves(HttpSession session,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "occurredAt,desc") String sort,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month) {
        Long buyerId = (Long) session.getAttribute("userId");
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getSavesByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getSaves(buyerId, pageable);
    }

    // 사용 내역: /api/points/uses?page=0&size=20&sort=occurredAt,desc
    @GetMapping("/uses")
    public Page<PointLedgerDto> getUses(HttpSession session,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "occurredAt,desc") String sort,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
        Long buyerId = (Long) session.getAttribute("userId");
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getUsesByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getUses(buyerId, pageable);
    }

    // 통합 내역: /api/points/history?page=0&size=20&sort=occurredAt,desc
    @GetMapping("/history")
    public Page<PointHistoryDto> getAll(HttpSession session,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "occurredAt,desc") String sort,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
        Long buyerId = (Long) session.getAttribute("userId");
        Sort sortObj = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        if (year != null && month != null) {
            LocalDateTime[] range = toMonthRange(year, month);
            return pointQueryService.getAllByMonth(buyerId, range[0], range[1], pageable);
        }
        return pointQueryService.getAll(buyerId, pageable);
    }

    // 이번 달 소멸 예정 합계(숫자만)
    @GetMapping("/expire/this-month/total")
    public ExpiringPointTotalDto getThisMonthExpiringTotal(HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        return pointQueryService.getThisMonthExpiringTotal(buyerId);
    }

    // 이번 달 소멸 예정 목록(항목 리스트)
    @GetMapping("/expire/this-month/list")
    public List<ExpiringPointItemDto> getThisMonthExpiringList(HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        return pointQueryService.getThisMonthExpiringList(buyerId);
    }

    // 특정 월의 소멸 예정 목록(항목 리스트)
    @GetMapping("/expire/list")
    public List<ExpiringPointItemDto> getMonthlyExpiringList(
            HttpSession session,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        Long buyerId = (Long) session.getAttribute("userId");
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
}