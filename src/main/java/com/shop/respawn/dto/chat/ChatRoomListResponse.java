package com.shop.respawn.dto.chat;

import java.time.LocalDateTime;

public record ChatRoomListResponse(
        String id,
        String buyerId,
        String buyerUsername,
        String sellerId,
        String sellerUsername,
        String itemId,
        String itemName,
        LocalDateTime createdAt,
        long unreadCount
) {}
