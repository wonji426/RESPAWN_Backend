package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Notice {

    @Id @GeneratedValue
    @Column(name = "notices_id")
    private Long id;

    private String title;

    private String description;

    private LocalDateTime createdAt;

    private int viewCount = 0;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Enumerated(STRING)
    private NoticeType noticeType;

    public static Notice createNotice(String title, String description, LocalDateTime createdAt, Admin admin, NoticeType noticeType) {
        Notice notice = new Notice();
        notice.title = title;
        notice.description = description;
        notice.createdAt = createdAt;
        notice.admin = admin;
        notice.noticeType = noticeType;
        return notice;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

}
