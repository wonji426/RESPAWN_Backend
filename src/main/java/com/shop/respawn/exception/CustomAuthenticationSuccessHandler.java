package com.shop.respawn.exception;

import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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

        switch (authentication.getAuthorities().toString()) {
            case "[ROLE_USER]" -> {
                long updated = buyerRepository.resetFailedLoginByUsername(username);
                log.debug("buyer reset: username={}, updated={}", username, updated);
            }
            case "[ROLE_SELLER]" -> {
                long updated = sellerRepository.resetFailedLoginByUsername(username);
                log.debug("seller reset: username={}, updated={}", username, updated);
            }
        }

        String target = "/loginOk";

        response.sendRedirect(target);
    }
}