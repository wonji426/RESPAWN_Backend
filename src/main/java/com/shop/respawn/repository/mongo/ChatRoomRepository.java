package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    // 특정 구매자, 판매자, 상품에 대한 채팅방이 이미 존재하는지 확인
    Optional<ChatRoom> findByBuyerIdAndSellerIdAndItemId(String buyerId, String sellerId, String itemId);

    List<ChatRoom> findBySellerId(String sellerId);
    List<ChatRoom> findByBuyerId(String buyerId);
}