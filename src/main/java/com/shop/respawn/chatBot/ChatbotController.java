package com.shop.respawn.chatBot;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatbotController {

    private final ChatService chatService;

    public ChatbotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/init-embeddings")
    public String initEmbeddings() {
        chatService.uploadEmbeddings();
        return "쇼핑몰 상품 임베딩 완료! 이제 몽고디비를 확인해보세요.";
    }

    /**
     * 사용자의 질문을 받아 제미나이 답변을 반환합니다.
     * 호출 예시: POST /api/chat/ask?message=게임용 키보드 추천해줘
     */
    @PostMapping("/ask")
    public String ask(@Valid @RequestBody ChatRequest request) {
        String message = request.getMessage();
        try {
            return chatService.getChatResponse(message);
        } catch (Exception e) {
            // 2. printStackTrace() 대신 log.error() 사용
            log.error("챗봇 응답 생성 중 오류 발생. 질문: {}", message, e);
            return "죄송합니다. 서버 내부 오류가 발생했습니다.";
        }
    }
}