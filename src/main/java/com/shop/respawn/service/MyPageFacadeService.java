package com.shop.respawn.service;

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

    @Transactional(readOnly = true)
    public MyPageSummaryDto getMyPageSummary(Long buyerId) {
        // 각 서비스나 리포지토리에서 단순 count만 가져옴
        long orderCount = orderService.countRecentYearOrders(buyerId);
        long couponCount = couponService.countAvailableCoupons(buyerId);
        long wishlistCount = wishlistService.countMyWishlist(buyerId);
        long reviewCount = reviewService.countMyReviews(buyerId);

        // 하나의 DTO로 묶어서 반환
        return new MyPageSummaryDto(orderCount, couponCount, wishlistCount, reviewCount);
    }
}
