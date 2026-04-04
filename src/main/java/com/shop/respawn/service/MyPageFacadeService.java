package com.shop.respawn.service;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.dto.MyPageSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageFacadeService {

    private final OrderService orderService;
    private final CouponService couponService;
    private final WishlistService wishlistService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final LedgerPointService ledgerPointService;

    @Transactional(readOnly = true)
    public MyPageSummaryDto getMyPageSummary(Long buyerId) {
        // 각 서비스나 리포지토리에서 단순 count만 가져옴
        long orderCount = orderService.countRecentYearOrders(buyerId);
        long couponCount = couponService.countAvailableCoupons(buyerId);
        long wishlistCount = wishlistService.countMyWishlist(buyerId);
        long reviewCount = reviewService.countMyReviews(buyerId);
        Grade userGrade = userService.getBuyerGrade(buyerId);
        long active = ledgerPointService.getActive(buyerId);

        // 하나의 DTO로 묶어서 반환
        return new MyPageSummaryDto(orderCount, couponCount, wishlistCount, reviewCount, userGrade, active);
    }
}
