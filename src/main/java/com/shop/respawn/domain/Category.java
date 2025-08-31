package com.shop.respawn.domain;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "categories")
@Getter @Setter
public class Category {
    @Id
    private String id;
    private String name;
    private ObjectId parent;
    private List<String> ancestors;
}