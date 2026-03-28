package com.shop.respawn.dto;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

public record PageResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<T> content,
        String error // 에러 메시지 필드 추가
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getContent(),
                null
        );
    }

    // 1) 기본 에러 응답 (page=0, size=0)
    public static <T> PageResponse<T> error(String message) {
        return new PageResponse<>(
                0,
                0,
                0L,
                0,
                true,
                true,
                Collections.emptyList(),
                message
        );
    }

    // 2) 호출자가 page/size를 지정하는 에러 응답
    public static <T> PageResponse<T> error(String message, int page, int size) {
        return new PageResponse<>(
                page,
                size,
                0L,
                0,
                page == 0,     // 에러 상황에서도 합리적 기본값
                true,
                Collections.emptyList(),
                message
        );
    }
}