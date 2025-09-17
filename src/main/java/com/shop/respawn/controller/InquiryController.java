package com.shop.respawn.controller;

import com.shop.respawn.dto.PageResponse;
import com.shop.respawn.dto.productInquiry.InquiryRequest;
import com.shop.respawn.dto.productInquiry.InquiryResponse;
import com.shop.respawn.dto.productInquiry.InquirySummaryResponse;
import com.shop.respawn.service.ItemService;
import com.shop.respawn.service.ProductInquiryService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.shop.respawn.util.AuthenticationUtil.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final ProductInquiryService productInquiryService;
    private final ItemService itemService;

    /**
     * 구매자 상품 문의 등록
     */
    @PostMapping
    public ResponseEntity<?> createInquiry(
            Authentication authentication,
            @RequestBody @Valid InquiryRequest dto
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            InquiryResponse created = productInquiryService.createInquiry(String.valueOf(buyerId), dto);
            return ResponseEntity.ok(Map.of("message", "상품 문의가 등록되었습니다.", "inquiry", created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 자신이 작성한 문의 조회
     */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<InquiryResponse>> getMyInquiries(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "questionDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
            Pageable pageable = PageRequest.of(page, size, sortSpec);
            Page<InquiryResponse> inquiries =
                    productInquiryService.getInquiriesByBuyer(String.valueOf(buyerId), pageable);
            return ResponseEntity.ok(PageResponse.from(inquiries));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 상품별 제목 조회
     */
    // 문의 스테이터스 오픈 투 퍼블릭으로
    @GetMapping("/{itemId}/titles")
    public ResponseEntity<PageResponse<InquirySummaryResponse>> getInquiryTitles(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "questionDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean openToPublic
    ) {
        try {
            Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
            Pageable pageable = PageRequest.of(page, size, sortSpec);
            Page<InquirySummaryResponse> result =
                    productInquiryService.getInquiryTitlesByItemId(itemId, status, openToPublic, pageable);
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    // 2) 문의 상세 조회: 구매자 본인 혹은 해당 상품 판매자만 접근 가능
    @GetMapping("/{inquiryId}/detail")
    public ResponseEntity<?> getInquiryDetail(
            Authentication authentication,
            @PathVariable String inquiryId
    ) {
        try {

            InquiryResponse inquiryDto = productInquiryService.getInquiryById(inquiryId);
            if (inquiryDto == null) {
                return ResponseEntity.notFound().build();
            }

            // 상품 판매자 ID 조회
            String itemId = inquiryDto.getItemId();
            String sellerId = itemService.getSellerIdByItemId(itemId);

            if(inquiryDto.isOpenToPublic()) {
                return ResponseEntity.ok(inquiryDto);
            } else {
                String userId = String.valueOf(getUserIdFromAuthentication(authentication));
                // 권한 체크: 로그인한 유저가 구매자 본인 OR 판매자면 허용
                if (userId.equals(inquiryDto.getBuyerId()) || userId.equals(sellerId)) {
                    return ResponseEntity.ok(inquiryDto);
                } else {
                    return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
                }
            }

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // 1) 판매자가 본인 상품에 대한 문의 목록 조회
    // 페이징 해야됨
    @GetMapping("/seller")
    public ResponseEntity<PageResponse<InquiryResponse>> getSellerInquiries(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "questionDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Long sellerId = getUserIdFromAuthentication(authentication);
        Sort sortSpec = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        Page<InquiryResponse> result = productInquiryService.getInquiriesBySellerId(String.valueOf(sellerId), pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    // 2) 판매자가 문의에 답변 등록/수정
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<?> answerInquiry(
            Authentication authentication,
            @PathVariable String inquiryId,
            @RequestBody Map<String, String> requestBody
    ) {
        try {
            String sellerId = String.valueOf(getUserIdFromAuthentication(authentication));

            String answer = requestBody.get("answer");
            if (answer == null || answer.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "답변 내용을 입력하세요."));
            }

            InquiryResponse updatedInquiry = productInquiryService.answerInquiry(inquiryId, answer, sellerId);
            return ResponseEntity.ok(Map.of("message", "답변이 등록되었습니다.", "inquiry", updatedInquiry));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

}
