package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.ChatMessage; // 앞서 만든 ChatMessage 엔티티 경로
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    // 특정 채팅방의 이전 메시지 내역을 시간순으로 불러올 때 사용할 메서드
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);

    List<ChatMessage> findByRoomId(String roomId);

    long countByRoomIdAndSenderIdNotAndIsReadFalse(String roomId, String senderId);
}