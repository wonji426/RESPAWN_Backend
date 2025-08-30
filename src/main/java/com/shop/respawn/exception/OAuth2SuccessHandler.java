package com.shop.respawn.exception;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.repository.BuyerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final BuyerRepository buyerRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();

        var buyer = buyerRepository.findByUsername(username);
        if (isIncompleteBuyer(buyer)) {
            String missing = buildMissingCsv(buyer);
            String parentUrl = "http://localhost:3000/profile/complete?missing=" +
                    URLEncoder.encode(missing, StandardCharsets.UTF_8);
            redirectParentAndClose(response, parentUrl); // 팝업 닫고 부모 이동
            return;
        }

        // 3) 누락 없으면 기존 목적지 처리(예: /loginOk로 세션 동기화)
        // SavedRequest를 쓰려면 SavedRequestAwareAuthenticationSuccessHandler 유사 로직을 적용
        redirectParentAndClose(response, "http://localhost:3000/loginOk");
    }

    private boolean isIncompleteBuyer(Buyer b) {
        // 필수 항목 정의: name/email/phoneNumber 중 하나라도 비어있으면 true
        return isBlank(b.getName()) || isBlank(b.getEmail()) || isBlank(b.getPhoneNumber());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String buildMissingCsv(Buyer b) {
        List<String> miss = new ArrayList<>();
        if (isBlank(b.getName())) miss.add("name");
        if (isBlank(b.getEmail())) miss.add("email");
        if (isBlank(b.getPhoneNumber())) miss.add("phoneNumber");
        return String.join(",", miss);
    }

    private void redirectParentAndClose(HttpServletResponse response, String parentUrl) throws IOException {
        String html = String.format("""
  <!doctype html><html><body>
  <script>
    try {
      if (window.opener && !window.opener.closed) {
        window.opener.location.href = '%s';
      }
    } catch (e) { }
    window.close();
  </script>
  </body></html>
  """, parentUrl);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().flush();
    }
}