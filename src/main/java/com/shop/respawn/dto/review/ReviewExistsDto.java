package com.shop.respawn.dto.review;

import lombok.Data;

@Data
public class ReviewExistsDto {

    private String reviewExists;
    private boolean exists;

    public ReviewExistsDto(String reviewExists, boolean exists) {
        this.reviewExists = reviewExists;
        this.exists = exists;
    }
}
