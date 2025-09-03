package com.shop.respawn.service;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Order;
import com.shop.respawn.dto.CouponDTO;
import com.shop.respawn.dto.coupon.CouponValidationResult;
import com.shop.respawn.repository.OrderRepository;
import com.shop.respawn.util.CouponPolicy;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Coupon;
import com.shop.respawn.repository.CouponRepository;
import com.shop.respawn.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final BuyerRepository buyerRepository;
    private final OrderRepository orderRepository;

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

    public long applyCouponIfValid(String code, long orderItemsAmount) {
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

        return coupon.getCouponAmount();
    }

    @Transactional(readOnly = true)
    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    public CouponValidationResult checkApplicableForOrder(Authentication authentication, Long orderId, String couponCode) {
        // 1) 주문 조회 + 소유자 검증
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());

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

        // 5) 통과
        return CouponValidationResult.ok(); // 비즈니스 결과 반환 [9]
    }

    public CouponValidationResult cancelApplicableForOrder(Authentication authentication, Long orderId) {
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId)); // 도메인 조회 [9]
        order.validateOwner(buyerId); // 비즈니스 규칙: 소유자 검증 [1]

        order.setTotalAmount(order.getOriginalAmount());
        order.setOriginalAmount(0L);

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
    public List<CouponDTO> getCouponDTOsByBuyerId(Authentication authentication) {
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName());
        List<Coupon> coupons = couponRepository.findAllUnusedByBuyerId(buyerId);
        return coupons.stream()
                .map(CouponDTO::fromEntity)
                .collect(Collectors.toList());
    }
}