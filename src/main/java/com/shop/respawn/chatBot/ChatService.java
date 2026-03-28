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

        List<String> filterList = buildFilterList(condition);

        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(userMessage)
                .topK(10);

        if (!filterList.isEmpty()) {
            String finalFilter = String.join(" && ", filterList);
            requestBuilder.filterExpression(finalFilter); // 여기서 필터 적용!
            System.out.println("적용된 동적 필터: " + finalFilter);
        }

        List<Document> similarDocuments = vectorStore.similaritySearch(requestBuilder.build());
        System.out.println("몽고DB에서 찾은 관련 상품 개수: " + similarDocuments.size());

        String productContext = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        if (productContext.isEmpty()) {
            productContext = "현재 설정하신 조건(가격, 브랜드 등)에 맞는 관련 상품 정보를 찾을 수 없습니다.";
        }

        String finalProductContext = productContext;
        return chatClient.prompt()
                .system(
                        "당신은 Respawn의 고객센터 AI챗봇입니다. " +
                                "제공된 [상품 정보] 내에서 사용자의 질문에 가장 알맞은 상품을 골라 친절하게 추천하세요. " +
                                "정보가 없으면 고객센터에 문의하라고 안내하세요. " +
                                "찾은 제품 중 가장 알맞은 제품 1~2개만 골라 사용장에게 보여주세요. "
                )
                .user(u -> u.text(userMessage + "\n\n[상품 정보]:\n" + finalProductContext))
                .call()
                .content();
    }

    /**
     * 임베딩 : 기존 DB의 상품들을 읽어서 벡터화한 뒤 다시 저장한다.
     */
    public void uploadEmbeddings() {
        List<Item> items = itemRepository.findAll();

        for (Item item : items) {
            try {
                // Document 생성 로직을 별도 메서드로 분리 (경고 수정)
                Document doc = createDocument(item);

                vectorStore.add(List.of(doc));
                System.out.println(item.getName() + " 임베딩 완료!");

                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println(item.getName() + " 처리 중 에러: " + e.getMessage());
            }
        }
    }

    /**
     * SearchCondition을 바탕으로 필터 리스트를 생성합니다.
     */
    private List<String> buildFilterList(SearchCondition condition) {
        List<String> filterList = new ArrayList<>();

        if (condition == null) {
            return filterList;
        }

        if (condition.maxPrice() != null)
            filterList.add("price <= " + condition.maxPrice());
        if (condition.minPrice() != null)
            filterList.add("price >= " + condition.minPrice());
        if (condition.name() != null && !condition.name().isEmpty())
            filterList.add("name == '" + condition.name() + "'");
        if (condition.deliveryType() != null && !condition.deliveryType().isEmpty())
            filterList.add("deliveryType == '" + condition.deliveryType() + "'");
        if (condition.deliveryFee() != null)
            filterList.add("deliveryFee == '" + condition.deliveryFee() + "'");
        if (condition.price() != null)
            filterList.add("price == '" + condition.price() + "'");
        if (condition.status() != null)
            filterList.add("status == '" + condition.status() + "'");
        if (condition.stockQuantity() != null)
            filterList.add("stockQuantity == '" + condition.stockQuantity() + "'");
        if (condition.company() != null && !condition.company().isEmpty())
            filterList.add("company == '" + condition.company() + "'");
        if (condition.category() != null && !condition.category().isEmpty())
            filterList.add("category == '" + condition.category() + "'");

        return filterList;
    }

    /**
     * Item 객체를 바탕으로 VectorStore에 저장할 Document를 생성합니다.
     */
    private Document createDocument(Item item) {
        String realCategoryName = "기타";
        if (item.getCategory() != null) {
            realCategoryName = categoryRepository.findById(String.valueOf(item.getCategory()))
                    .map(Category::getName)
                    .orElse("기타");
        }

        String content = String.format("상품명: %s, 가격: %d, 설명: %s",
                item.getName(), item.getPrice(), item.getDescription());

        return new Document(content, Map.of(
                "itemId", item.getId(),
                "name", item.getName(),
                "deliveryType", item.getDeliveryType(),
                "deliveryFee", item.getDeliveryFee(),
                "price", item.getPrice(),
                "company", item.getCompany(),
                "status", item.getStatus(),
                "category", realCategoryName,
                "stockQuantity", item.getStockQuantity()
        ));
    }
}
