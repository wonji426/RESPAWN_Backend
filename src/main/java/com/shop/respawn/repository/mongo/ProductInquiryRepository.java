package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.ProductInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductInquiryRepository extends MongoRepository<ProductInquiry, String> {

    List<ProductInquiry> findAllByItemIdOrderByQuestionDateDesc(String itemId);

    List<ProductInquiry> findAllByItemIdInOrderByQuestionDateDesc(List<String> itemIds);

    Page<ProductInquiry> findByBuyerId(String buyerId, Pageable pageable);
}
