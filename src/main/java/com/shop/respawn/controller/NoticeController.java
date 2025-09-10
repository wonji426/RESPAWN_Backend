package com.shop.respawn.controller;

import com.shop.respawn.dto.NoticeDto;
import com.shop.respawn.dto.NoticeResponse;
import com.shop.respawn.dto.NoticeSummaryDto;
import com.shop.respawn.dto.user.PageResponse;
import com.shop.respawn.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.shop.respawn.util.AuthenticationUtil.getUserIdFromAuthentication;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 공지사항 등록 컨트롤러
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerItem(
            Authentication authentication,
            @RequestBody NoticeDto noticeDto
    ) {
        try {
            Long adminId = getUserIdFromAuthentication(authentication);
            noticeService.CreateNotice(adminId, noticeDto);
            return ResponseEntity.ok().body("공지사항 등록 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("공지사항 등록 에러: " + e.getMessage());
        }
    }

    /**
     * 공지사항 목록 조회 컨트롤러 페이징(제목, 공지사항 타입, 생성시간)
     */
    @GetMapping("/summaries")
    public ResponseEntity<PageResponse<NoticeSummaryDto>> getNoticeSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NoticeSummaryDto> summaries = noticeService.getNoticeSummaries(pageable);
        return ResponseEntity.ok(PageResponse.from(summaries));
    }

    /**
     * 공지사항 조회
     */
    @GetMapping("/view")
    public ResponseEntity<NoticeResponse> getNotices(@RequestParam Long noticeId) {
        try {
            NoticeResponse notice = noticeService.getNotice(noticeId);
            return ResponseEntity.ok().body(notice);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
