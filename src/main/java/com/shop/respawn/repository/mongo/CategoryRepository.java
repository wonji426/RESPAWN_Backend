package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String> {
}
