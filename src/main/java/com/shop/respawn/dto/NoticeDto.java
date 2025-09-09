package com.shop.respawn.dto;

import com.shop.respawn.domain.NoticeType;
import lombok.Data;

@Data
public class NoticeDto {

    private String title;

    private String description;

    private NoticeType noticeType;

    public NoticeDto(String title, String description, NoticeType noticeType) {
        this.title = title;
        this.description = description;
        this.noticeType = noticeType;
    }
}
