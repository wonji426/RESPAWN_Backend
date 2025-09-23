package com.shop.respawn.service;

import com.shop.respawn.domain.Admin;
import com.shop.respawn.domain.Notice;
import com.shop.respawn.dto.notice.NoticeDto;
import com.shop.respawn.dto.notice.NoticeResponse;
import com.shop.respawn.dto.notice.NoticeSummaryDto;
import com.shop.respawn.repository.jpa.AdminRepository;
import com.shop.respawn.repository.jpa.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AdminRepository adminRepository;

    /**
     * 공지사항 등록 메서드
     */
    public void CreateNotice(Long adminId, NoticeDto noticeDto) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자 계정을 찾을 수 없습니다."));

        Notice notice = Notice.createNotice(
                noticeDto.getTitle(),
                noticeDto.getDescription(),
                LocalDateTime.now(),
                admin,
                noticeDto.getNoticeType()
        );

        noticeRepository.save(notice);
    }

    /**
     * 공지사항 목록 조회 메서드 페이징 (제목, 공지사항 타입, 생성시간)
     */
    public Page<NoticeSummaryDto> getNoticeSummaries(Pageable pageable) {
        return noticeRepository.findNoticeSummaries(pageable);
    }


    /**
     * 공지사항 조회 메서드
     */
    public NoticeResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
        increaseNoticeViewCount(noticeId);
        return NoticeResponse.of(
                notice.getTitle(),
                notice.getDescription(),
                notice.getCreatedAt(),
                notice.getViewCount(),
                notice.getAdmin().getName(),
                notice.getNoticeType()
        );
    }

    /**
     * 공지사항 조회 시 조회수 증가 메서드
     */
    public void increaseNoticeViewCount(Long noticeId) {
        // 동시성 문제 사전 차단 ( UPDATE notices SET view_count = view_count + 1 WHERE id = :id )
        noticeRepository.incrementViewCount(noticeId);
    }
}
