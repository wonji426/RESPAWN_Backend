package com.shop.respawn.chatBot;

import com.shop.respawn.domain.Item;
import com.shop.respawn.repository.mongo.ItemRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ItemRepository itemRepository;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore, ItemRepository itemRepository) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.itemRepository = itemRepository;
    }

    public String getChatResponse(String userMessage) {
        // 1. 검색 수행 (결과가 null일 가능성이 있음)
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(3)
                        .build()
        );
        // ✨ 여기에 이 한 줄을 추가해서 콘솔 창을 확인해 보세요!
        System.out.println("🤖 몽고DB에서 찾은 관련 상품 개수: " + similarDocuments.size());

        // 2. null 체크 후 스트림 처리 (Optional 활용)
        String productContext = Optional.ofNullable(similarDocuments)
                .orElse(Collections.emptyList()) // null이면 빈 리스트 반환
                .stream()
                .map(doc -> {
                    // 데이터 추출 시에도 안전하게 처리
                    String name = (String) doc.getMetadata().getOrDefault("name", "상품명 없음");
                    Object price = doc.getMetadata().getOrDefault("price", 0);
                    return String.format("상품명: %s, 가격: %s원, 설명: %s",
                            name, price, doc.getText());
                })
                .collect(Collectors.joining("\n\n"));

        // 3. 만약 검색 결과가 전혀 없다면 안내 문구 추가
        if (productContext.isEmpty()) {
            productContext = "현재 관련 상품 정보를 찾을 수 없습니다.";
        }

        String finalProductContext = productContext;
        return chatClient.prompt()
                .system("당신은 Respawn의 고객센터 AI챗봇입니다. 제공된 정보를 바탕으로 답변하세요." +
                        " 그리고 항상 친절하고 매너있게 답변하세요" +
                        "또한 모르는 사항이 있으면 고객센터로 문의해주세요하고 답변하세요.")
                .user(u -> u.text(userMessage + "\n\n[상품 정보]:\n" + finalProductContext))
                .call()
                .content();
    }

    // 💡 핵심: 기존 DB의 상품들을 읽어서 벡터화한 뒤 다시 저장합니다.
    public void uploadEmbeddings() {
        List<Item> items = itemRepository.findAll();

        for (Item item : items) {
            try {
                // 1. 상품 정보를 간결하게 정리 (너무 길면 에러 납니다)
                String content = String.format("상품명: %s, 가격: %d, 설명: %s",
                        item.getName(), item.getPrice(), item.getDescription());

                // 2. 상품 하나당 하나의 Document 생성
                Document doc = new Document(content, Map.of("itemId", item.getId()));

                // 3. 하나씩 저장 (속도는 조금 느리지만 토큰 에러를 피할 수 있습니다)
                vectorStore.add(List.of(doc));

                System.out.println(item.getName() + " 임베딩 완료!");

                // 4. API 과부하 방지를 위한 아주 잠깐의 휴식 (선택 사항)
                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println(item.getName() + " 처리 중 에러: " + e.getMessage());
            }
        }
    }
}
