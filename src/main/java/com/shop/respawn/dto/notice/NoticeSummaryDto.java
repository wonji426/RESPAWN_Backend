package com.shop.respawn.dto.notice;

import com.querydsl.core.annotations.QueryProjection;
import com.shop.respawn.domain.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeSummaryDto {
    private Long id;
    private String title;
    private NoticeType noticeType;
    private LocalDateTime createdAt;

    @QueryProjection
    public NoticeSummaryDto(Long id, String title, NoticeType noticeType, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.noticeType = noticeType;
        this.createdAt = createdAt;
    }

}