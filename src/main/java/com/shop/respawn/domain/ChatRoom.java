package com.shop.respawn.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_rooms")
public record ChatRoom(
        @Id String id,
        String buyerId,    // 구매자 아이디
        String sellerId,   // 판매자 아이디
        String itemId,       // 관련 상품 ID (선택 사항)
        LocalDateTime createdAt
) {}