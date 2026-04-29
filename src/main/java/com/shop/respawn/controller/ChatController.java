package com.shop.respawn.controller;

import com.shop.respawn.domain.ChatMessage;
import com.shop.respawn.dto.chat.ChatMessageDTO;
import com.shop.respawn.dto.chat.ChatRoomListResponse;
import com.shop.respawn.dto.chat.ChatRoomRequest;
import com.shop.respawn.dto.chat.ChatRoomResponse;
import com.shop.respawn.repository.mongo.ChatMessageRepository;
import com.shop.respawn.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static com.shop.respawn.util.AuthenticationUtil.getUserIdFromAuthentication;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/send/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId, ChatMessageDTO messageDTO, Principal principal) {

        // 1. 발송자 이름 세팅 (시큐리티 세션 기반)
        String senderId = (principal != null) ? principal.getName() : "알 수 없는 사용자";

        // 2. MongoDB에 저장할 엔티티 생성
        ChatMessage message = new ChatMessage(
                null,
                roomId,
                senderId,
                messageDTO.content(),
                messageDTO.type(),
                false,
                LocalDateTime.now()
        );

        if ("CHAT".equals(messageDTO.type())) {
            chatMessageRepository.save(message);
        }

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody ChatRoomRequest request, Authentication authentication) {

        // 시큐리티 세션을 통해 현재 로그인한 사용자의 ID(구매자)를 가져옵니다.
        Long buyerId = getUserIdFromAuthentication(authentication);

        // 서비스 로직 호출 (방 생성 또는 기존 방 조회)
        String roomId = chatRoomService.createOrGetChatRoom(
                String.valueOf(buyerId),
                request.sellerId(),
                request.itemId()
        );

        // 프론트엔드로 roomId 반환
        return ResponseEntity.ok(new ChatRoomResponse(roomId));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getRooms(Authentication authentication) {
        Long sellerId = getUserIdFromAuthentication(authentication);

        List<ChatRoomListResponse> rooms = chatRoomService.getSellerChatRooms(sellerId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/buyer/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getBuyerRooms(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);

        List<ChatRoomListResponse> rooms = chatRoomService.getBuyerChatRooms(buyerId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable String roomId) {
        // Repository에 이미 만들어둔 findByRoomIdOrderByTimestampAsc를 활용합니다.
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        return ResponseEntity.ok(messages);
    }

    // 방에 입장할 때 상대방이 보낸 메시지들을 '읽음'으로 업데이트
    @PatchMapping("/room/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String roomId, Principal principal) {
        String currentUserId = principal.getName();

        // 1. DB 업데이트 (상대방 메시지들을 읽음 처리)
        chatRoomService.updateMessagesAsRead(roomId, currentUserId);

        // 2. ⭐️ 실시간 알림 전송: 상대방 화면의 '1'을 지우기 위해 'READ' 타입 메시지 브로드캐스팅
        ChatMessage readNotification = new ChatMessage(
                null,
                roomId,
                currentUserId,  // 읽은 사람 ID
                "READ_EVENT",   // 더미 메시지
                "READ",         // ⭐️ 타입을 'READ'로 보냄
                true,
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, readNotification);

        return ResponseEntity.ok().build();
    }
}