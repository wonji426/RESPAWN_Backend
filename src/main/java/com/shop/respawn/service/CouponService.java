package com.shop.respawn.service;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Order;
import com.shop.respawn.dto.coupon.*;
import com.shop.respawn.repository.jpa.OrderRepository;
import com.shop.respawn.util.CouponPolicy;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Coupon;
import com.shop.respawn.repository.jpa.CouponRepository;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final BuyerRepository buyerRepository;
    private final OrderRepository orderRepository;
    private final RedisUtil redisUtil;

    // 등급 변경 시 쿠폰 발급
    public void issueGradeCoupon(Long buyerId, Grade tier) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 없음: " + buyerId));

        LocalDateTime now = LocalDateTime.now();

        long couponAmount = CouponPolicy.couponAmount(tier);

        Coupon coupon = Coupon.builder()
                .buyer(buyer)
                .code(uniqueCode())
                .name(CouponPolicy.couponName(tier))
                .couponAmount(couponAmount)
                .issuedAt(now)
                .expiresAt(CouponPolicy.defaultExpiry(now))
                .used(false)
                .build();

        couponRepository.save(coupon);
    }

    public void applyCouponIfValid(String code, long orderItemsAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));

        if (coupon.isUsed()) {
            throw new RuntimeException("이미 사용된 쿠폰입니다.");
        }
        if (coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("쿠폰 사용기한이 만료되었습니다.");
        }
        if (orderItemsAmount <= coupon.getCouponAmount()) {
            throw new RuntimeException("상품 금액이 쿠폰 금액보다 커야 합니다.");
        }

        coupon.markUsed();
        couponRepository.save(coupon); // 변경감지로도 가능하나 명시 저장
    }

    @Transactional(readOnly = true)
    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    public CouponValidationResult checkApplicableForOrder(Long buyerId, Long orderId, String couponCode) {
        // 1) 주문 조회 + 소유자 검증
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId)); // 도메인 조회 [9]
        order.validateOwner(buyerId); // 비즈니스 규칙: 소유자 검증 [1]

        // 2) 총 상품금액(배송비 제외) 계산
        long totalItemAmount = order.getOrderItems().stream()
                .mapToLong(oi -> oi.getOrderPrice() * oi.getCount())
                .sum(); // 도메인 규칙 계산 [1]

        // 3) 쿠폰 엔티티 조회 및 기본 상태 검증 (사용/만료)
        var couponOpt = getCouponByCode(couponCode); // 사용 처리 없이 조회용 [7]
        if (couponOpt.isEmpty()) {
            return CouponValidationResult.fail("쿠폰을 찾을 수 없습니다."); // 상태 검증 [1]
        }
        var coupon = couponOpt.get();

        if (coupon.isUsed()) {
            return CouponValidationResult.fail("이미 사용된 쿠폰입니다."); // 사용 여부 검증 [1]
        }
        if (coupon.getExpiresAt() == null || !coupon.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
            return CouponValidationResult.fail("만료된 쿠폰입니다."); // 만료 검증 [1]
        }

        // 4) 금액 정책: 배송비 제외 아이템 합계가 쿠폰 금액보다 커야 함
        if (totalItemAmount <= coupon.getCouponAmount()) {
            return CouponValidationResult.fail("상품 금액이 쿠폰 금액보다 커야 합니다."); // 금액 정책 [1]
        }

        order.setTotalAmount(order.getTotalAmount() - coupon.getCouponAmount());

        String redisKey = "order:" + orderId + ":couponAmount";
        redisUtil.setData(redisKey, String.valueOf(coupon.getCouponAmount()));

        // 5) 통과
        return CouponValidationResult.ok(); // 비즈니스 결과 반환 [9]
    }

    public CouponValidationResult cancelApplicableForOrder(Long buyerId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId)); // 도메인 조회 [9]
        order.validateOwner(buyerId); // 비즈니스 규칙: 소유자 검증 [1]

        String redisKey = "order:" + orderId + ":couponAmount";
        String data = redisUtil.getData(redisKey);
        order.setTotalAmount(order.getTotalAmount() + Long.parseLong(data));
        redisUtil.deleteData(redisKey);

        // 5) 통과
        return CouponValidationResult.ok(); // 비즈니스 결과 반환 [9]
    }

    // 쿠폰 코드 중복 검사
    private String uniqueCode() {
        String code;
        do {
            code = CouponPolicy.generateCode();
        } while (couponRepository.findByCode(code).isPresent());
        return code;
    }

    @Transactional(readOnly = true)
    public CouponCountDto countCouponsByBuyerId(Long buyerId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponStatusDto> coupons = couponRepository.findAllByBuyerIdQueryDsl(buyerId);
        int availableCount = (int) coupons.stream()
                .filter(c -> !c.used() && c.expiresAt() != null && c.expiresAt().isAfter(now))
                .count();
        int unavailableCount = (int) coupons.stream()
                .filter(c -> c.used() || c.expiresAt() == null || !c.expiresAt().isAfter(now))
                .count();
        return new CouponCountDto(availableCount, unavailableCount);
    }

    @Transactional(readOnly = true)
    public Page<CouponDTO> getAvailableCouponsByBuyerId(Long buyerId, Pageable pageable) {
        return couponRepository.findAllAvailableByBuyerId(buyerId, pageable)
                .map(CouponDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CouponDTO> getUnavailableCouponsByBuyerId(Long buyerId, Pageable pageable) {
        return couponRepository.findAllUnavailableByBuyerId(buyerId, pageable)
                .map(CouponDTO::fromEntity);
    }

    public List<CouponUsageStatusDto> getCouponsUsageStatusByBuyerAndOrder(Long buyerId, Long orderId) {
        List<Coupon> coupons = couponRepository.findAllByBuyerId(buyerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        long totalItemAmount = order.getOrderItems().stream()
                .mapToLong(oi -> oi.getOrderPrice() * oi.getCount())
                .sum();

        List<CouponUsageStatusDto> result = new ArrayList<>();

        for (Coupon coupon : coupons) {
            CouponDTO dto = CouponDTO.fromEntity(coupon);
            String failReason = null;

            if (coupon.isUsed()) {
                failReason = "이미 사용된 쿠폰입니다.";
            } else if (coupon.getExpiresAt() == null || !coupon.getExpiresAt().isAfter(LocalDateTime.now())) {
                failReason = "만료된 쿠폰입니다.";
            } else if (totalItemAmount <= coupon.getCouponAmount()) {
                failReason = "상품 금액이 쿠폰 금액보다 커야 합니다.";
            }
            boolean usable = (failReason == null);
            result.add(CouponUsageStatusDto.create(dto, usable, failReason));
        }

        return result;
    }
}