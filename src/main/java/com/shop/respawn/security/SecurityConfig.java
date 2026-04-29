package com.shop.respawn.security;

import com.shop.respawn.exception.*;
import com.shop.respawn.security.oauth.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 필터가 스프링 필터체인에 등록됨
@EnableMethodSecurity(securedEnabled = true) // secured 어노테이션 활성화, preAuthorize 어노테이션 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final PrincipalOauth2UserService principalOauth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable) //csrf 검증 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));// ⭐️ CORS 설정 추가

        /*
            requestMatchers에 특정 url을 입력하면 해당 url에 대한 접근 권한을 설정
            permitAll(): 인증받지 않은 유저(로그인 하지 않은 유저)에게도 접근을 허용
            hasRole(): 특정 권한을 갖고 있는 유저에게만 접근을 허용
            hasAnyRole(): 인수에 전달한 여러 권한 중 하나라도 가진 유저에게 접근을 허용
         */
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/bring-me").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/ws/chat/**").authenticated()
                        .requestMatchers("/api/uploads/**").permitAll()
                        .anyRequest().permitAll()
                );

        http //일반 로그인
                .formLogin(auth -> auth
                        .loginPage("/login")
                        .loginProcessingUrl("/api/loginProc")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/api/loginOk")
                        .successHandler(customAuthenticationSuccessHandler) // 성공 핸들러
                        .failureHandler(customAuthenticationFailureHandler) // 실패 핸들러
                        .permitAll()
                );

        http
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        // 프론트에서 소셜 로그인 창을 띄울 때 쓸 시작 주소 변경
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/api/oauth2/authorization")
                        )
                        // 구글/카카오가 로그인 코드를 던져줄 리디렉트 주소 변경
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/api/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(principalOauth2UserService)
                        )
                        .defaultSuccessUrl(frontendUrl + "/loginOk")
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                        .permitAll()
                );


        http //로그아웃
                .logout(logout -> logout
                        .logoutUrl("/api/logout")                           // 로그아웃 요청 URL (기본값: "/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .logoutSuccessUrl("/api/logoutOk")                  // 로그아웃 성공 후 리다이렉트 URL (기본값: "/login?logout")
                        .invalidateHttpSession(true)                      // 세션 무효화 (기본값: true)
                        .deleteCookies("JSESSIONID")     // 쿠키 삭제
                );

        http
                .exceptionHandling(ex -> ex
                        // 401 Unauthorized (인증이 안 된 상태로 보호된 API에 접근할 때)
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        // 403 Forbidden (인증은 되었으나 권한(Role)이 부족할 때)
                        .accessDeniedHandler(restAccessDeniedHandler())
                );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // 필요한 경우만 세션 생성
                        .sessionFixation().changeSessionId()                      // 세션 고정 공격 방지
                        .maximumSessions(1)                                       // 최대 동시 로그인 1개
                        .maxSessionsPreventsLogin(true)                       // 기존 세션 유지, 새 로그인 차단
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // ★ withCredentials: true와 같이 사용하려면 꼭 true로 설정
        config.setAllowedOrigins(List.of(frontendUrl)); // * 사용하지 말고 정확하게 지정
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}