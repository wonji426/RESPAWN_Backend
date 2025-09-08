package com.shop.respawn.util;

import com.shop.respawn.security.auth.PrincipalDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SessionUtil {

    /**
     * 세션에서 buyerId를 가져오는 헬퍼 메서드
     */
    public static Long getBuyerIdFromSession(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authorities = authentication.getAuthorities().toString();
        if(authorities.equals("[ROLE_USER]")){
            return (Long) session.getAttribute("userId");
        } else throw new RuntimeException("로그인이 필요하거나 판매자 아이디 입니다.");
    }

    public static Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof PrincipalDetails) {
            return ((PrincipalDetails) principal).getUserId();
        } else throw new RuntimeException("로그인이 필요합니다.");
    }

}
