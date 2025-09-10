package com.shop.respawn.exception;

import com.shop.respawn.dto.query.FailureResultDto;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String username = request.getParameter("username");
        FailureResult result = null;

        FailureResultDto buyerDto = buyerRepository.increaseFailedAttemptsAndGetStatus(username);
        if (buyerDto != null) {
            result = toFailureResult(buyerDto);
        } else {
            FailureResultDto sellerDto = sellerRepository.increaseFailedAttemptsAndGetStatus(username);
            if (sellerDto != null) {
                result = toFailureResult(sellerDto);
            }
        }

        writeFailureResponse(response, result);
    }

    private FailureResult toFailureResult(FailureResultDto dto) {
        FailureResult result = new FailureResult();
        result.disabled = dto.isDisabled();
        result.expired = dto.isExpired();
        result.locked = dto.isLocked();
        result.failedAttempts = dto.getFailedAttempts();
        return result;
    }

    private void writeFailureResponse(HttpServletResponse response, FailureResult result) throws IOException {
        boolean disabled = result != null && result.disabled;
        boolean expired  = result != null && result.expired;
        boolean locked   = result != null && result.locked;
        int attempts     = result != null ? result.failedAttempts : 0;

        String errorCode = disabled ? "disabled"
                : expired ? "expired"
                : locked ? "locked"
                : "invalid_credentials";

        int attemptsForResponse = disabled ? 0 : attempts;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"error\":\"%s\", \"failedLoginAttempts\": %d}", errorCode, attemptsForResponse)
        );
    }

    private static class FailureResult {
        boolean disabled;
        boolean locked;
        boolean expired;
        int failedAttempts;
    }
}
