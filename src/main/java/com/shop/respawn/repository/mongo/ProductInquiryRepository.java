package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.ProductInquiry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductInquiryRepository extends MongoRepository<ProductInquiry, String> {

    List<ProductInquiry> findAllByItemIdOrderByQuestionDateDesc(String itemId);

    List<ProductInquiry> findAllByBuyerIdOrderByQuestionDateDesc(String buyerId);

    List<ProductInquiry> findAllByItemIdInOrderByQuestionDateDesc(List<String> itemIds);
}
