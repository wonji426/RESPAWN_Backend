package com.shop.respawn.chatBot;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder().build();

    @PostMapping("/ask")
    public Mono<String> askChatbot(@RequestBody ChatRequest request) {
        String model = "gemini-2.5-flash"; // 작동 확인된 모델 사용
        String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key=" + apiKey;

        // 예시 프롬프트 구성
        String systemInstruction = """
                너는 'Respawn'의 상담원이야. 아래 제공된 [웹사이트 정보]만을 바탕으로 답변해줘. 항상 친절하고 매너있게 답변해줘. 모르는 내용은 '고객센터로 문의해주세요'라고만 답해.
                
                [웹사이트 정보]
                - 각종 컴퓨터 주변기기(마우스, 키보드, 헤드셋 등)을 판매하는 쇼핑몰
                - 게임기 같은 물건도 판매중
                - 반품 기간: 수령 후 7일 이내
                - 환불 문의는 고객센터를 이용
                """;

        String payload = "{\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\""
                + systemInstruction + "\\n\\n질문: " + request.getMessage() + "\"}]}]}";

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class) // JsonNode로 받기
                .map(jsonNode -> {
                    // 응답 JSON 구조: candidates[0].content.parts[0].text 추출
                    return jsonNode.path("candidates")
                            .get(0)
                            .path("content")
                            .path("parts")
                            .get(0)
                            .path("text")
                            .asText();
                })
                .onErrorReturn("죄송합니다. 답변을 생성하는 중에 문제가 발생했습니다.");
    }
}