package com.shop.respawn.dto.productInquiry;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InquiryRequest {

    @NotBlank
    private String itemId;

    @NotBlank
    private String inquiryType;

    @NotBlank
    private String question;

    @NotBlank
    private String questionDetail;

    private boolean openToPublic;

}