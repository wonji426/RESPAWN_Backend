package com.shop.respawn.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public record ChatMessage(
        @Id String id,
        String roomId,          // 소속된 채팅방 ID
        String senderId,        // 발신자 아이디
        String message,         // 내용
        String type,            //
        boolean isRead,         //
        LocalDateTime timestamp //
) {}