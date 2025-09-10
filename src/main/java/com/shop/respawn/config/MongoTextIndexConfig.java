package com.shop.respawn.config;

import com.shop.respawn.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import static org.springframework.data.mongodb.core.index.TextIndexDefinition.*;

@Configuration
@RequiredArgsConstructor
public class MongoTextIndexConfig {
    private final MongoTemplate mongoTemplate;

    @EventListener(ContextRefreshedEvent.class)
    public void ensureTextIndex() {
        TextIndexDefinition textIndex = new TextIndexDefinitionBuilder()
                .onField("name", 2F)          // 중요도 가중치
                .onField("description")
                .onField("company")
                .onField("tags")
                .build();
        mongoTemplate.indexOps(Item.class).createIndex(textIndex);
    }
}