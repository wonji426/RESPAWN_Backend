package com.shop.respawn.dto;

import com.shop.respawn.domain.ItemStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@NoArgsConstructor
public class ItemDto {

    private String id;

    private String name;

    private String description;

    private String deliveryType;

    private Long deliveryFee;

    private String company;

    private Long companyNumber;

    private Long price;

    private long stockQuantity;

    private String sellerId;

    private String imageUrl;

    private ObjectId category;

    private ItemStatus status;

    public ItemDto(String id, String name, String description, String deliveryType, Long deliveryFee, String company, Long companyNumber, Long price, long stockQuantity, String sellerId, String imageUrl, ObjectId category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.deliveryType = deliveryType;
        this.deliveryFee = deliveryFee;
        this.company = company;
        this.companyNumber = companyNumber;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public ItemDto(String id, String name, String description, String deliveryType, Long deliveryFee, String company, Long companyNumber, Long price, long stockQuantity, String sellerId, String imageUrl, ObjectId category, ItemStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.deliveryType = deliveryType;
        this.deliveryFee = deliveryFee;
        this.company = company;
        this.companyNumber = companyNumber;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
        this.category = category;
        this.status = status;
    }

}
