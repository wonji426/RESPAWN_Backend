package com.shop.respawn.chatBot;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "질문을 입력하세요")
    private String message;

    // 필요 시 추가 필드 (예: userId, categoryFilter 등)
    // private String userId;
}