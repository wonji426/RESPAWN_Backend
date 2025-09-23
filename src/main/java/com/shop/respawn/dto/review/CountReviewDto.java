package com.shop.respawn.dto.review;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CountReviewDto {

    private long writableCount;
    private long writtenCount;

    public static CountReviewDto of(Long writableCount, Long writtenCount) {
        CountReviewDto dto = new CountReviewDto();
        dto.writableCount = writableCount;
        dto.writtenCount = writtenCount;
        return dto;
    }
}
