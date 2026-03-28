package com.shop.respawn.dto.notice;

import com.shop.respawn.domain.NoticeType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NoticeUpdateDto {
    private String title;
    private String description;
    private NoticeType noticeType;
}