package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.shop.respawn.domain.ItemStatus.*;

@Document(collection = "item")
@Getter @Setter
public class Item {

    @Id
    private String id;

    private String name;
    @Column(columnDefinition = "TEXT")
    private String deliveryType;
    private Long deliveryFee;
    private String company;
    private Long companyNumber;
    private Long price;
    private long stockQuantity;
    private long soldCount = 0L;
    private String sellerId;
    private String imageUrl;
    private ObjectId category;
    private String description;
    private ItemStatus status = SALE;
    private long wishCount = 0;
    private LocalDateTime createdAt;
    private long reviewCount = 0L;
    private List<String> tags = new ArrayList<>();

    //==비즈니스 로직==//
    public void addStock(long quantity) {
        this.stockQuantity += quantity;
    }

    public void removeStock(long quantity) {
        long restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new RuntimeException("재고가 부족합니다");
        }
        this.stockQuantity = restStock;
    }

    public void addWishCount() {
        this.wishCount++;
    }

    public void removeWishCount() {
        if (this.wishCount > 0) {
            this.wishCount--;
        }
    }

    public void addSoldCount(long quantity) {
        this.soldCount += quantity;
    }

    public void removeSoldCount(long quantity) {
        this.soldCount -= quantity;

        if (this.soldCount < 0) {
            this.soldCount = 0L;
        }
    }

    public void addReviewCount() {
        this.reviewCount++;
    }

    public void removeReviewCount() {
        if (this.reviewCount > 0) {
            this.reviewCount--;
        }
    }
}
