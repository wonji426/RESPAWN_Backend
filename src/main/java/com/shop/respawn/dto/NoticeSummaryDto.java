package com.shop.respawn.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.shop.respawn.domain.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeSummaryDto {
    private String title;
    private NoticeType noticeType;
    private LocalDateTime createdAt;

    @QueryProjection
    public NoticeSummaryDto(String title, NoticeType noticeType, LocalDateTime createdAt) {
        this.title = title;
        this.noticeType = noticeType;
        this.createdAt = createdAt;
    }

}