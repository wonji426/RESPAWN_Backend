package com.shop.respawn.dto.chat;

public record ChatRoomRequest(
        String itemId,  // ⭐️ Long을 String으로 변경
        String sellerId
) {}