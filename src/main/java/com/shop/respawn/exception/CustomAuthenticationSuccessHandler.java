package com.shop.respawn.exception;

import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().toString();

        String loginType = request.getParameter("loginType");
        boolean isFromAdminPage = "admin".equals(loginType);

        if (authorities.contains("ROLE_ADMIN")) {
            if (!isFromAdminPage) {
                log.warn("관리자 계정으로 일반 로그인 시도 차단: {}", username);
                blockLogin(request, response, "관리자는 일반 로그인 페이지를 이용할 수 없습니다.");
                return;
            }
        } else {
            if (isFromAdminPage) {
                log.warn("일반 사용자 계정으로 관리자 로그인 시도 차단: {}", username);
                blockLogin(request, response, "일반 사용자는 관리자 페이지에 로그인할 수 없습니다.");
                return;
            }

            switch (authorities) {
                case "[ROLE_USER]" -> buyerRepository.resetFailedLoginByUsername(username);
                case "[ROLE_SELLER]" -> sellerRepository.resetFailedLoginByUsername(username);
            }
        }

        if (isFromAdminPage) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"message\": \"로그인 성공\", \"role\": \"ROLE_ADMIN\", \"username\": \"%s\"}",
                    username
            ));
        } else {
            response.sendRedirect("/api/loginOk");
        }
    }

    private void blockLogin(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + errorMessage + "\"}");
    }
}