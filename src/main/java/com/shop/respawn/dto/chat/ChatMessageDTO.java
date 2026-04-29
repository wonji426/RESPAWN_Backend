package com.shop.respawn.dto.chat;

public record ChatMessageDTO(
        String content,
        String type // 필요하다면 CHAT, JOIN, LEAVE 등의 타입을 추가
) {}
