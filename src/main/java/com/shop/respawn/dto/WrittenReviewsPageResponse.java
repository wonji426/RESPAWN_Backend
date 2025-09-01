package com.shop.respawn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WrittenReviewsPageResponse {
    private List<ReviewWithItemDto> items;
    private long total;
}
