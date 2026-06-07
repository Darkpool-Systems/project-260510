package com.back.auth.oauth;

import com.back.auth.domain.Provider;
import com.back.auth.domain.Role;
import com.back.auth.domain.User;
import com.back.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth2 사용자 정보 처리 서비스
 * - Google 로그인 성공 시 호출됨
 * - 신규 사용자 → DB에 저장 + isNewUser=true
 * - 기존 사용자 → 닉네임 업데이트 + isNewUser=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");

        // 신규 사용자 여부 판단
        boolean isNewUser = !userRepository.existsByEmail(email);

        // 기존 사용자 → 그대로 반환 (닉네임은 우리 서비스에서만 관리)
        // 신규 사용자 → Google 이름으로 초기 닉네임 세팅 후 DB 저장
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .nickname(name)
                                .provider(Provider.GOOGLE)
                                .providerId(providerId)
                                .role(Role.USER)
                                .build()
                ));

        log.info("OAuth2 로그인 - email: {}, 신규여부: {}", user.getEmail(), isNewUser);

        return new CustomOAuth2User(user, attributes, isNewUser);
    }
}
