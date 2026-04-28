package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.chat.ChatRoomListResponse;
import com.shop.respawn.repository.UserRepositoryCustom;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import com.shop.respawn.repository.mongo.ChatMessageRepository;
import com.shop.respawn.repository.mongo.ChatRoomRepository;
import com.shop.respawn.repository.mongo.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final ItemRepository itemRepository;

    public String createOrGetChatRoom(String buyerId, String sellerId, String itemId) {
        // 1. 방어 로직: 구매자와 판매자가 같은 경우 (자신의 상품에 문의하기 클릭)
        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("자신이 등록한 상품에는 채팅을 할 수 없습니다.");
        }

        // 2. 기존 채팅방이 있는지 조회
        return chatRoomRepository.findByBuyerIdAndSellerIdAndItemId(buyerId, sellerId, itemId)
                .map(ChatRoom::id) // 방이 존재하면 해당 방의 ID 반환
                .orElseGet(() -> {
                    // 방이 없으면 새로 생성 후 MongoDB에 저장
                    ChatRoom newRoom = new ChatRoom(
                            null, // MongoDB가 ObjectId를 자동 생성하므로 null
                            buyerId,
                            sellerId,
                            itemId,
                            LocalDateTime.now()
                    );
                    ChatRoom savedRoom = chatRoomRepository.save(newRoom);
                    return savedRoom.id(); // 새로 생성된 방의 ID 반환
                });
    }

    public void updateMessagesAsRead(String roomId, String currentUserId) {
        // 1. 해당 방의 메시지 중 내가 보내지 않았고(상대방이 보냄), 아직 읽지 않은 메시지들을 가져옵니다.
        List<ChatMessage> unreadMessages = chatMessageRepository.findByRoomId(roomId)
                .stream()
                .filter(msg -> !msg.senderId().equals(currentUserId) && !msg.isRead())
                .toList();

        // 2. 읽음 상태로 변경 후 저장합니다.
        if (!unreadMessages.isEmpty()) {
            List<ChatMessage> readMessages = unreadMessages.stream()
                    .map(msg -> new ChatMessage(
                            msg.id(),
                            msg.roomId(),
                            msg.senderId(),
                            msg.message(),
                            msg.type(),
                            true,
                            msg.timestamp()
                    ))
                    .toList();
            chatMessageRepository.saveAll(readMessages);
        }
    }

    public List<ChatRoomListResponse> getSellerChatRooms(Long sellerId) {
        String sellerIdStr = String.valueOf(sellerId);
        String sellerUsername = sellerRepository.findById(sellerId)
                .map(Seller::getUsername)
                .orElse("알 수 없는 사용자");
        return chatRoomRepository.findBySellerId(sellerIdStr).stream()
                .map(room -> {
                    long unreadCount = chatMessageRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.id(), sellerIdStr);
                    String buyerUsername = buyerRepository.findById(Long.valueOf(room.buyerId()))
                            .map(Buyer::getUsername)
                            .orElse("알 수 없는 사용자");
                    String itemName = "상품 없음";
                    if (room.itemId() != null) {
                        itemName = itemRepository.findById(room.itemId())
                                .map(Item::getName) // 상품명 가져오는 메서드(getName, getTitle 등)
                                .orElse("삭제된 상품");
                    }
                    return new ChatRoomListResponse(
                            room.id(), room.buyerId(), buyerUsername, room.sellerId(), sellerUsername, room.itemId(), itemName, room.createdAt(), unreadCount
                    );
                })
                .collect(Collectors.toList());
    }

    public List<ChatRoomListResponse> getBuyerChatRooms(Long buyerId) {
        String buyerIdStr = String.valueOf(buyerId);
        String buyerUsername = buyerRepository.findById(buyerId)
                .map(Buyer::getUsername)
                .orElse("알 수 없는 사용자");
        return chatRoomRepository.findByBuyerId(buyerIdStr).stream()
                .map(room -> {
                    long unreadCount = chatMessageRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.id(), buyerIdStr);
                    String sellerUsername = sellerRepository.findById(Long.valueOf(room.sellerId()))
                            .map(Seller::getUsername)
                            .orElse("알 수 없는 사용자");
                    String itemName = "상품 없음";
                    if (room.itemId() != null) {
                        itemName = itemRepository.findById(room.itemId())
                                .map(Item::getName) // 상품명 가져오는 메서드(getName, getTitle 등)
                                .orElse("삭제된 상품");
                    }
                    return new ChatRoomListResponse(
                            room.id(), room.buyerId(), buyerUsername, room.sellerId(), sellerUsername, room.itemId(), itemName, room.createdAt(), unreadCount
                    );
                })
                .collect(Collectors.toList());
    }
}
