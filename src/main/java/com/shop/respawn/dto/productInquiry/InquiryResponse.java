package com.shop.respawn.dto.productInquiry;

import com.shop.respawn.domain.ProductInquiry;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class InquiryResponse {

    private String id;

    private String buyerId;

    private String buyerUsername;

    private String itemId;

    private String itemName;

    private String inquiryType;

    private String question;

    private String questionDetail;

    private String answer;

    private LocalDateTime questionDate;

    private LocalDateTime answerDate;

    private String status;

    private boolean openToPublic;

    public static InquiryResponse of(ProductInquiry inquiry, String itemName, String buyerUsername) {
        InquiryResponse response = new InquiryResponse();
        response.setId(inquiry.getId());
        response.setBuyerId(inquiry.getBuyerId());
        response.setBuyerUsername(buyerUsername);
        response.setItemId(inquiry.getItemId());
        response.setItemName(itemName);
        response.setInquiryType(inquiry.getInquiryType());
        response.setQuestion(inquiry.getQuestion());
        response.setQuestionDetail(inquiry.getQuestionDetail());
        response.setAnswer(inquiry.getAnswer());
        response.setQuestionDate(inquiry.getQuestionDate());
        response.setAnswerDate(inquiry.getAnswerDate());
        response.setStatus(inquiry.getStatus().name());
        response.setOpenToPublic(inquiry.isOpenToPublic());
        return response;
    }

    public static InquiryResponse of(ProductInquiry inquiry, Map<String, String> itemIdToName) {
        InquiryResponse response = new InquiryResponse();
        response.setId(inquiry.getId());
        response.setBuyerId(inquiry.getBuyerId());
        response.setBuyerUsername(null); // 필요 시 별도 배치 조회/캐시로 주입 권장
        response.setItemId(inquiry.getItemId());
        response.setItemName(itemIdToName.getOrDefault(inquiry.getItemId(), ""));
        response.setInquiryType(inquiry.getInquiryType());
        response.setQuestion(inquiry.getQuestion());
        response.setQuestionDetail(inquiry.getQuestionDetail());
        response.setAnswer(inquiry.getAnswer());
        response.setQuestionDate(inquiry.getQuestionDate());
        response.setAnswerDate(inquiry.getAnswerDate());
        response.setStatus(inquiry.getStatus().name());
        response.setOpenToPublic(inquiry.isOpenToPublic());
        return response;
    }

    public static InquiryResponse of(ProductInquiry inquiry, String buyerUsername) {
        InquiryResponse response = new InquiryResponse();
        response.setId(inquiry.getId());
        response.setBuyerId(inquiry.getBuyerId());
        response.setBuyerUsername(buyerUsername);
        response.setItemId(inquiry.getItemId());
        response.setInquiryType(inquiry.getInquiryType());
        response.setQuestion(inquiry.getQuestion());
        response.setQuestionDetail(inquiry.getQuestionDetail());
        response.setAnswer(inquiry.getAnswer());
        response.setQuestionDate(inquiry.getQuestionDate());
        response.setAnswerDate(inquiry.getAnswerDate());
        response.setStatus(inquiry.getStatus().name());
        response.setOpenToPublic(inquiry.isOpenToPublic());
        return response;
    }

}
