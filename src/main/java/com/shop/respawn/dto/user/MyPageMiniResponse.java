package com.shop.respawn.dto.user;

import com.shop.respawn.domain.Grade;

/**
 * @param activePoint 사용가능 포인트
 * @param couponCount 사용가능 쿠폰 개수
 * @param grade       현재 등급
 */
public record MyPageMiniResponse(Long activePoint, Long couponCount, Grade grade) {
}
