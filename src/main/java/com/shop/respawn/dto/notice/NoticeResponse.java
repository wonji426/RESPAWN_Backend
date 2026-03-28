package com.shop.respawn.dto.notice;

import com.shop.respawn.domain.NoticeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeResponse {

    private String title;

    private String description;

    private LocalDateTime createdAt;

    private int viewCount = 0;

    private String adminName;

    private NoticeType noticeType;

    public static NoticeResponse of(String title, String description, LocalDateTime createdAt, int viewCount, String adminName, NoticeType noticeType) {
        NoticeResponse noticeResponse = new NoticeResponse();
        noticeResponse.setTitle(title);
        noticeResponse.setDescription(description);
        noticeResponse.setCreatedAt(createdAt);
        noticeResponse.setViewCount(viewCount);
        noticeResponse.setAdminName(adminName);
        noticeResponse.setNoticeType(noticeType);
        return noticeResponse;
    }
}
