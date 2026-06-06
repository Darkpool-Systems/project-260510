package com.back.domain.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 * - 실패 사유를 통제된 에러 코드로 변환하여 프론트엔드로 리다이렉트
 * - 원시 예외 메시지(스택/내부 정보) 노출 금지
 *
 * 에러 코드 목록:
 *   access_denied                  - 사용자가 구글 동의 화면에서 취소/거부
 *   authorization_request_not_found - state 불일치 / 세션 만료 (CSRF 검증 실패)
 *   invalid_token_response          - 토큰 교환 실패
 *   invalid_user_info_response      - 사용자 프로필 조회 실패
 *   oauth_failed                    - 그 외 일반 실패 (기본값)
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = "oauth_failed";

        if (exception instanceof OAuth2AuthenticationException oae) {
            String errorCode = oae.getError().getErrorCode();
            if (errorCode != null && !errorCode.isBlank()) {
                code = errorCode;
            }
        }

        log.warn("OAuth2 로그인 실패 - errorCode: {}", code);

        String target = UriComponentsBuilder
                .fromUriString(frontendUrl + "/login")
                .queryParam("error", code)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
