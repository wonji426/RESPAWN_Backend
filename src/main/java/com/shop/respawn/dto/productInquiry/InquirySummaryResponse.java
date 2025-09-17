package com.shop.respawn.dto.productInquiry;

import com.shop.respawn.domain.ProductInquiry;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InquirySummaryResponse {

    private String id;

    private String buyerId;

    private String buyerUsername;

    private String itemId;

    private String inquiryType;

    private String question;

    private LocalDateTime questionDate;

    private String status;

    private boolean openToPublic;

    public static InquirySummaryResponse of(ProductInquiry inquiry) {
        InquirySummaryResponse dto = new InquirySummaryResponse();
        dto.setId(inquiry.getId());
        dto.setBuyerId(inquiry.getBuyerId());
        // username이 꼭 필요 없다면 제외해 네트워크/DB 호출 최소화
        dto.setItemId(inquiry.getItemId());
        dto.setInquiryType(inquiry.getInquiryType());
        dto.setQuestion(inquiry.getQuestion());
        dto.setQuestionDate(inquiry.getQuestionDate());
        dto.setStatus(inquiry.getStatus().name());
        dto.setOpenToPublic(inquiry.isOpenToPublic());
        return dto;
    }

}
