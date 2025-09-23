package com.shop.respawn.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하이어야 합니다.")
    private int rating;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자까지 작성할 수 있습니다.")
    private String content;

    // 기본 생성자
    private ReviewRequestDto() {
    }

}
