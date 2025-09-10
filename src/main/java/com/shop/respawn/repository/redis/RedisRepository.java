package com.shop.respawn.repository.redis;

import com.shop.respawn.domain.Redis;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface RedisRepository extends KeyValueRepository<Redis, String> {
}
