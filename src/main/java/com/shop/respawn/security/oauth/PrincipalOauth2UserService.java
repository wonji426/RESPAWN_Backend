package com.shop.respawn.security.oauth;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Role;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.security.auth.PrincipalDetails;
import com.shop.respawn.security.oauth.provider.GoogleUserInfo;
import com.shop.respawn.security.oauth.provider.KakaoUserInfo;
import com.shop.respawn.security.oauth.provider.NaverUserInfo;
import com.shop.respawn.security.oauth.provider.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final BuyerRepository buyerRepository;
    private final BCryptPasswordEncoder encoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // registrationId로 어떤 OAuth로 로그인 했는지 확인 가능
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // 구글로그인 버튼 클릭 -> 구글로그인창 -> 로그인을 완료 -> code를 리턴(OAuth2-Client 라이브러리) -> AccessToken 요청
        // userRequest 정보 -> 회원 프로필 받아야함(loadUser함수 호출) -> 구글로부터 회원프로필 받아준다.

        OAuth2UserInfo oAuth2UserInfo = null;
        switch (userRequest.getClientRegistration().getRegistrationId()) {
            case "google" -> {
                System.out.println("구글 로그인 요청");
                oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
            }
            case "naver" -> {
                System.out.println("네이버 로그인 요청");
                Object responseObj = oAuth2User.getAttributes().get("response");
                if (responseObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = (Map<String, Object>) responseObj;
                    oAuth2UserInfo = new NaverUserInfo(response);
                } else {
                    throw new IllegalArgumentException("네이버 response 데이터 타입이 올바르지 않습니다.");
                }
            }
            case "kakao" -> {
                System.out.println("카카오 로그인 요청");
                oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
            }
            default -> System.out.println("구글, 네이버, 카카오 로그인을 지원합니다.");
        }

        assert oAuth2UserInfo != null;
        String provider = oAuth2UserInfo.getProvider(); // google
        String providerId = oAuth2UserInfo.getProviderId();
        String name = oAuth2UserInfo.getName();
        String username = provider + "_" + providerId; // google_10021320120
        String password = encoder.encode("겟인데어");
        String email = oAuth2UserInfo.getEmail();
        String phoneNumber = oAuth2UserInfo.getPhoneNumber();
        Role role = Role.ROLE_USER;
        Grade grade = Grade.BASIC;

        Buyer buyer = buyerRepository.findByUsername(username);
        if (buyer == null) {
            if(!buyerRepository.existsUserIdentityConflict(email, phoneNumber, username)){
                Buyer newBuyer = Buyer.builder()
                        .name(name)
                        .username(username)
                        .password(password)
                        .email(email)
                        .role(role)
                        .grade(grade)
                        .provider(provider)
                        .providerId(providerId)
                        .build();
                // 비번 기준 시각 초기화 보정
                if (newBuyer.getAccountStatus().getLastPasswordChangedAt() == null) {
                    newBuyer.getAccountStatus().markPasswordChangedNow();
                }
                newBuyer.renewExpiryDate(); // 정책에 따라 유지
                buyerRepository.save(newBuyer);
                return new PrincipalDetails(newBuyer, oAuth2User.getAttributes());
            } else {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("social_id_conflict"),
                        "해당 이메일 또는 전화번호로 가입된 아이디가 있습니다."
                );
            }
        } else {
            return new PrincipalDetails(buyer, oAuth2User.getAttributes());
        }
    }
}
