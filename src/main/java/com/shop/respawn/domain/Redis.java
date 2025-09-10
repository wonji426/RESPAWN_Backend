package com.shop.respawn.domain;

import jakarta.persistence.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("redis")
public class Redis {

    @Id
    private String id;

    private String value;
}
