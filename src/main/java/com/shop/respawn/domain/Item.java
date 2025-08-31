package com.shop.respawn.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String sellerId;
    private String imageUrl;
    private ObjectId category;
    private String description;
    private ItemStatus status = SALE;
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
}
