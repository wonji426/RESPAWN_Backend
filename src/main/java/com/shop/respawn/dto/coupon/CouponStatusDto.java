package com.shop.respawn.dto.coupon;

import java.time.LocalDateTime;

public record CouponStatusDto(boolean used, LocalDateTime expiresAt) {
}