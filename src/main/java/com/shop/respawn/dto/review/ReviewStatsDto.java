package com.shop.respawn.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDto {
    private String itemId;
    private double averageRating; // 평균 점수
    private long totalReviews;    // 총 리뷰 개수
}
