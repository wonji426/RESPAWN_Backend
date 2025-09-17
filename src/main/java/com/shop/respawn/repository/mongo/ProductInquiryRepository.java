package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.ProductInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductInquiryRepository extends MongoRepository<ProductInquiry, String> {

    Page<ProductInquiry> findByBuyerId(String buyerId, Pageable pageable);

    Page<ProductInquiry> findByItemId(String itemId, Pageable pageable);

    Page<ProductInquiry> findByItemIdIn(List<String> itemIds, Pageable pageable);

}
