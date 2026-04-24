package com.shop.respawn.dto.chat;

import java.time.LocalDateTime;

public record ChatRoomListResponse(
        String id,
        String buyerId,
        String sellerId,
        String itemId,
        LocalDateTime createdAt,
        long unreadCount
) {}
