package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ItemRepository extends MongoRepository<Item, String>, ItemRepositoryCustom {
    List<Item> findBySellerId(String sellerId);

}
