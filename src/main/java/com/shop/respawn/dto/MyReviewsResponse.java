package com.shop.respawn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyReviewsResponse {
    private List<WritableReviewDto> writableItems;
    private List<ReviewWithItemDto> writtenReviews;
}
