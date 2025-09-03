package com.shop.respawn.dto;

import com.shop.respawn.domain.Item;
import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.Review;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewWithItemDto {

    private String reviewId;
    private String buyerId;
    private String maskedUsername;
    private String orderItemId;
    private int rating;
    private String content;
    private LocalDateTime createdDate;

    // 주문일시
    private LocalDateTime orderDate;

    // 아이템 정보 (필요한 항목만)
    private String itemId;
    private String itemName;
    private String imageUrl;
    private Long price;

    public ReviewWithItemDto(Review review, Item item, String maskedUsername, Order order) {
        this.reviewId = review.getId();
        this.buyerId = review.getBuyerId();
        this.maskedUsername = maskedUsername;
        this.orderItemId = review.getOrderItemId();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.createdDate = review.getCreatedDate();
        this.orderDate = order != null ? order.getOrderDate() : null;

        if(item != null){
            this.itemId = item.getId();
            this.itemName = item.getName();
            this.imageUrl = item.getImageUrl();
            this.price = item.getPrice();
        }
    }
}
