package com.shop.respawn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WritableReviewsPageResponse {
    private List<WritableReviewDto> items;  // 현재 페이지 항목
    private long totalWritable;             // 리뷰 작성가능 갯수
    private long totalWritten;              // 리뷰 작성한 갯수
}