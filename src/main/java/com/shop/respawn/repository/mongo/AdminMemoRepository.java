package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.AdminMemo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminMemoRepository extends MongoRepository<AdminMemo, String> {

    Optional<AdminMemo> findByUserTypeAndUserId(String targetType, Long targetId);
}