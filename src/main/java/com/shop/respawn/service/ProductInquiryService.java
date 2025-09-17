package com.shop.respawn.service;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.InquiryStatus;
import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.ProductInquiry;
import com.shop.respawn.dto.productInquiry.InquiryRequest;
import com.shop.respawn.dto.productInquiry.InquiryResponse;
import com.shop.respawn.dto.productInquiry.InquirySummaryResponse;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.mongo.ProductInquiryRepository;
import com.shop.respawn.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductInquiryService {

    private final ProductInquiryRepository productInquiryRepository;
    private final BuyerRepository buyerRepository;
    private final ItemService itemService;

    // 문의 ID로 상세 조회
    public InquiryResponse getInquiryById(String inquiryId) {
        // 1) 문의 조회
        ProductInquiry inquiry = productInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의가 존재하지 않습니다."));

        // 2) 구매자 username 조회 및 마스킹
        Buyer buyer = buyerRepository.findById(Long.valueOf(inquiry.getBuyerId()))
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다."));
        String maskedUsername = MaskingUtil.maskUsername(buyer.getUsername());

        // 3) DTO 생성 (정적 팩토리)
        return InquiryResponse.of(inquiry, maskedUsername);
    }

    //상품별 제목 조회
    public Page<InquirySummaryResponse> getInquiryTitlesByItemId(String itemId, Pageable pageable) {
        // 정렬을 Pageable로 전달하는 버전 권장
        Page<ProductInquiry> page = productInquiryRepository.findByItemId(itemId, pageable);

        List<InquirySummaryResponse> content = page.getContent().stream()
                .map(InquirySummaryResponse::of)
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    // 상품 문의 등록
    public InquiryResponse createInquiry(String buyerId, InquiryRequest dto) {

        ProductInquiry inquiry = new ProductInquiry();
        inquiry.setBuyerId(buyerId);
        inquiry.setItemId(dto.getItemId());
        inquiry.setInquiryType(dto.getInquiryType());
        inquiry.setQuestion(dto.getQuestion());
        inquiry.setQuestionDetail(dto.getQuestionDetail());
        inquiry.setQuestionDate(LocalDateTime.now());
        inquiry.setStatus(InquiryStatus.WAITING);
        inquiry.setOpenToPublic(dto.isOpenToPublic());

        ProductInquiry saved = productInquiryRepository.save(inquiry);
        String buyerUsername = buyerRepository.findById(Long.valueOf(buyerId))
                .map(Buyer::getUsername)
                .orElse("알 수 없음");

        return InquiryResponse.of(saved, buyerUsername);
    }

    public Page<InquiryResponse> getInquiriesByBuyer(String buyerId, Pageable pageable) {
        // 1) 문의 페이징 조회
        Page<ProductInquiry> page = productInquiryRepository.findByBuyerId(buyerId, pageable);

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2) itemIds 추출(distinct)
        List<String> itemIds = page.getContent().stream()
                .map(ProductInquiry::getItemId)
                .distinct()
                .toList();

        // 3) Item 일괄 조회 (부분 필드 조회 있으면 우선 사용)
        List<Item> items = itemService.getItemsByIds(itemIds); // 또는 getPartialItemsByIds
        Map<String, String> itemIdToName = items.stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        String buyerUsername = buyerRepository.findById(Long.valueOf(buyerId))
                .map(Buyer::getUsername)
                .orElse("알 수 없음");

        // 4) DTO 매핑
        List<InquiryResponse> content = page.getContent().stream()
                .map(productInquiry -> InquiryResponse
                        .of(
                                productInquiry,
                                itemIdToName.getOrDefault(productInquiry.getItemId(), "알 수 없는 상품"),
                                buyerUsername
                        )
                )
                .toList();

        // 5) Page로 래핑
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    // 판매자의 상품에 달린 모든 문의 조회
    public Page<InquiryResponse> getInquiriesBySellerId(String sellerId, Pageable pageable) {
        // 1) 판매자의 아이템 조회
        List<Item> sellerItems = itemService.getItemsBySellerId(sellerId);
        if (sellerItems.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> sellerItemIds = sellerItems.stream().map(Item::getId).toList();

        // 2) 문의 페이징 조회 (정렬은 pageable)
        Page<ProductInquiry> page = productInquiryRepository.findByItemIdIn(sellerItemIds, pageable);

        // 3) itemId → itemName 매핑
        Map<String, String> itemIdToName = sellerItems.stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        // 4) DTO 변환
        List<InquiryResponse> content = page.getContent().stream()
                .map(productInquiry -> InquiryResponse.of(productInquiry, itemIdToName))
        .toList();

        // 5) Page 래핑
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    // 판매자가 문의에 답변 등록
    public InquiryResponse answerInquiry(String inquiryId, String answer, String sellerId) {
        ProductInquiry inquiry = productInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의가 존재하지 않습니다."));

        // 1) 문의된 상품의 판매자 확인
        String itemId = inquiry.getItemId();
        String inquirySellerId = itemService.getSellerIdByItemId(itemId);

        // 2) 현재 요청한 판매자와 문의 상품의 판매자 일치 확인
        if (!sellerId.equals(inquirySellerId)) {
            throw new RuntimeException("해당 문의에 대한 답변 권한이 없습니다.");
        }

        // 3) 답변 등록 및 상태 변경
        inquiry.setAnswer(answer);
        inquiry.setAnswerDate(LocalDateTime.now());
        inquiry.setStatus(InquiryStatus.ANSWERED);

        ProductInquiry saved = productInquiryRepository.save(inquiry);
        String buyerId = saved.getBuyerId();
        String buyerUsername = buyerRepository.findById(Long.valueOf(buyerId))
                .map(Buyer::getUsername)
                .orElse("알 수 없음");
        return InquiryResponse.of(saved, buyerUsername);
    }

}
