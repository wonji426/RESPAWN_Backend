package com.shop.respawn.controller;

import com.shop.respawn.dto.MyPageSummaryDto;
import com.shop.respawn.service.MyPageFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.shop.respawn.util.AuthenticationUtil.getUserIdFromAuthentication;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageFacadeService myPageFacadeService;

    /**
     * 마이페이지 요약 데이터 단일 조회 (주문 갯수, 사용 가능한 쿠폰 수, 찜 갯수, 작성한 리뷰 수)
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getMyPageSummary(Authentication authentication) {
        try {
            // 토큰을 통해 로그인한 유저의 ID 추출
            Long buyerId = getUserIdFromAuthentication(authentication);

            // Facade 서비스를 호출하여 4개의 데이터를 한 번에 가져옴
            MyPageSummaryDto summary = myPageFacadeService.getMyPageSummary(buyerId);

            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
