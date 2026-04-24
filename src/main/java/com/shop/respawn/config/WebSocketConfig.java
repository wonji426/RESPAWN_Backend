package com.shop.respawn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // SecurityConfig와 동일하게 환경 변수에서 프론트 주소를 가져옵니다.
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 프론트엔드 요청에 맞게 /api 접두사를 추가합니다.
        registry.addEndpoint("/api/ws/chat")
                .setAllowedOriginPatterns("*")
                // 2. SecurityConfig와 동일한 출처(Origin)를 허용하여 CORS 충돌을 방지합니다.
                .setAllowedOrigins(frontendUrl, "http://localhost:3000")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}