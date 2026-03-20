package com.shop.respawn.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException ex) throws IOException {

        String reason = "auth_failed";
        if (ex instanceof OAuth2AuthenticationException oae) {
            String code = (oae.getError() != null) ? oae.getError().getErrorCode() : null;
            System.out.println("code = " + code);
            if ("social_id_conflict".equals(code)) {
                System.out.println("code = " + code);
                reason = "account_conflict"; // 프런트 합의값
            }
        }

        // postMessage로 부모창에 전달하고 팝업 닫기
        String html = getString(reason);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().flush();
    }

    @NotNull
    private static String getString(String reason) {
        String payload = String.format("{\"type\":\"OAUTH_FAIL\",\"reason\":\"%s\"}", reason);
        return """
          <!doctype html><html><body>
          <script>
            (function(){
              var data = %s;
              try {
                if (window.opener && !window.opener.closed) {
                  window.opener.postMessage(data, "http://respawnstore.shop");
                }
              } catch(e) {}
              window.close();
            })();
          </script>
          </body></html>
        """.formatted(payload);
    }
}