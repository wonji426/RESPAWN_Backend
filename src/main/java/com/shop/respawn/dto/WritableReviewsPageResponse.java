package com.shop.respawn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WritableReviewsPageResponse {
    private List<WritableReviewDto> items; // 현재 페이지 항목
    private long total;                    // 전체 개수
}