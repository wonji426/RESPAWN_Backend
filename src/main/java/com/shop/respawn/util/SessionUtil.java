package com.shop.respawn.util;

import com.shop.respawn.security.auth.PrincipalDetails;
import org.springframework.security.core.Authentication;

public class SessionUtil {

    /**
     * Authentication에서 userId를 가져오는 헬퍼 메서드
     */
    public static Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof PrincipalDetails) {
            return ((PrincipalDetails) principal).getUserId();
        } else throw new RuntimeException("로그인이 필요합니다.");
    }

}
