package com.shop.respawn.dto;

import java.time.LocalDateTime;

public interface ReviewLite {
    String getId();
    String getBuyerId();
    String getOrderItemId();
    String getItemId();
    int getRating();
    String getContent();
    LocalDateTime getCreatedDate();
}
