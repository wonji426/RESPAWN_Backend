package com.shop.respawn.chatBot;

import com.shop.respawn.domain.Category;
import com.shop.respawn.domain.Item;
import com.shop.respawn.repository.mongo.CategoryRepository;
import com.shop.respawn.repository.mongo.ItemRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore, ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    public String getChatResponse(String userMessage) {

        // ==========================================================
        // 1단계: AI에게 사용자의 질문을 분석하라고 명령 (조건 추출)
        // ==========================================================
        SearchCondition condition = chatClient.prompt()
                .system("당신은 쇼핑몰 검색 조건 추출기입니다. " +
                        "사용자의 질문에서 최대 가격(maxPrice), " +
                        "최소 가격(minPrice), " +
                        "이름(name), " +
                        "배송방법(deliveryType), " +
                        "배송비(deliveryFee), " +
                        "가격(price), " +
                        "판매상태(status: SALE, PAUSED, STOPPED), " +
                        "재고(stockQuantity), " +
                        "브랜드명(company: 영문으로 변환 해야할 것 같으면 영문으로 변환), " +
                        "카테고리(category: 마우스, 키보드 등)를 추출하세요. " +
                        "해당 조건이 없으면 null을 반환하세요.")
                .user(userMessage)
                .call()
                .entity(SearchCondition.class);

        System.out.println("🤖 AI가 분석한 검색 조건: " + condition);

        // ==========================================================
        // 2단계: 추출된 조건을 직관적인 '문자열'로 조립하기
        // ==========================================================
        List<String> filterList = new ArrayList<>();

        if (condition != null) {
            // 가격 조건 추가
            if (condition.maxPrice() != null) {
                filterList.add("price <= " + condition.maxPrice());
            }
            if (condition.minPrice() != null) {
                filterList.add("price >= " + condition.minPrice());
            }
            if (condition.name() != null && !condition.name().isEmpty()) {
                filterList.add("name == '" + condition.name() + "'");
            }
            if (condition.deliveryType() != null && !condition.deliveryType().isEmpty()) {
                filterList.add("deliveryType == '" + condition.deliveryType() + "'");
            }
            if (condition.deliveryFee() != null) {
                filterList.add("deliveryFee == '" + condition.deliveryFee() + "'");
            }
            if (condition.price() != null) {
                filterList.add("price == '" + condition.price() + "'");
            }
            if (condition.status() != null) {
                filterList.add("status == '" + condition.status() + "'");
            }
            if (condition.stockQuantity() != null) {
                filterList.add("stockQuantity == '" + condition.stockQuantity() + "'");
            }
            // 회사명 조건 추가 (따옴표 주의!)
            if (condition.company() != null && !condition.company().isEmpty()) {
                filterList.add("company == '" + condition.company() + "'");
            }
            // 카테고리 조건 추가 (선택 사항)
            if (condition.category() != null && !condition.category().isEmpty()) {
                filterList.add("category == '" + condition.category() + "'");
            }
        }

        // ==========================================================
        // 3단계: 빌더(Builder)에 조건 달고 검색하기
        // ==========================================================
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(userMessage)
                .topK(10);

        // 만들어둔 조건이 하나라도 있다면 &&(AND) 기호로 이어 붙여줍니다.
        if (!filterList.isEmpty()) {
            String finalFilter = String.join(" && ", filterList);
            requestBuilder.filterExpression(finalFilter); // 여기서 필터 적용!
            System.out.println("🤖 적용된 동적 필터: " + finalFilter);
        }

        // 검색 실행
        List<Document> similarDocuments = vectorStore.similaritySearch(requestBuilder.build());
        System.out.println("🤖 몽고DB에서 찾은 관련 상품 개수: " + similarDocuments.size());

        // ==========================================================
        // 4단계: 잃어버렸던 productContext 만들기! (검색 결과를 텍스트로)
        // ==========================================================
        String productContext = Optional.ofNullable(similarDocuments)
                .orElse(Collections.emptyList())
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 만약 필터 조건이 너무 빡빡해서 검색 결과가 0개라면?
        if (productContext.isEmpty()) {
            productContext = "현재 설정하신 조건(가격, 브랜드 등)에 맞는 관련 상품 정보를 찾을 수 없습니다.";
        }

        // ==========================================================
        // 5단계: 걸러진 상품 정보를 바탕으로 최종 답변 생성
        // ==========================================================
        String finalProductContext = productContext;
        return chatClient.prompt()
                .system("당신은 Respawn의 고객센터 AI챗봇입니다. 제공된 [상품 정보] 내에서 사용자의 질문에 가장 알맞은 상품을 골라 친절하게 추천하세요. 정보가 없으면 고객센터에 문의하라고 안내하세요.")
                .user(u -> u.text(userMessage + "\n\n[상품 정보]:\n" + finalProductContext))
                .call()
                .content();
    }

    // 💡 핵심: 기존 DB의 상품들을 읽어서 벡터화한 뒤 다시 저장합니다.
    public void uploadEmbeddings() {
        List<Item> items = itemRepository.findAll();

        for (Item item : items) {
            try {

                String realCategoryName = "기타"; // 기본값
                if (item.getCategory() != null) {
                    realCategoryName = categoryRepository.findById(String.valueOf(item.getCategory()))
                            .map(Category::getName) // 💡 Category 엔티티의 진짜 이름 필드 (혹시 title 이라면 getTitle()로 변경!)
                            .orElse("기타");
                }

                // 1. 상품 정보를 간결하게 정리 (너무 길면 에러 납니다)
                String content = String.format("상품명: %s, 가격: %d, 설명: %s",
                        item.getName(), item.getPrice(), item.getDescription());

                // 2. 상품 하나당 하나의 Document 생성
                Document doc = new Document(content, Map.of(
                        "itemId", item.getId(),
                        "name", item.getName(),
                        "deliveryType", item.getDeliveryType(),
                        "deliveryFee", item.getDeliveryFee(),
                        "price", item.getPrice(),
                        "company", item.getCompany(),
                        "status", item.getStatus(),
                        "category", realCategoryName, // ObjectId인 경우 문자열 변환
                        "stockQuantity", item.getStockQuantity()
                ));

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
