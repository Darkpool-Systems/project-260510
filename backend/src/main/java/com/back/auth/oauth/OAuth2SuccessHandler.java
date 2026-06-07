package com.back.auth.oauth;

import com.back.auth.jwt.JwtTokenProvider;
import com.back.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 핸들러
 * - JWT 생성 → HttpOnly 쿠키에 저장
 * - 신규 사용자 → frontend-new-user-url 로 리다이렉트
 * - 기존 사용자 → frontend-url 로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.frontend-new-user-url}")
    private String frontendNewUserUrl;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();
        boolean isNewUser = oAuth2User.isNewUser();

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole().getKey()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail(), user.getRole().getKey()
        );

        log.info("JWT 발급 완료 - userId: {}, 신규여부: {}", user.getId(), isNewUser);

        // HttpOnly 쿠키로 토큰 설정
        addCookie(response, "access_token", accessToken, (int) (accessTokenExpiry / 1000));
        addCookie(response, "refresh_token", refreshToken, (int) (refreshTokenExpiry / 1000));

        // 신규 사용자 → /welcome, 기존 사용자 → /
        String redirectUrl = isNewUser ? frontendNewUserUrl : frontendUrl;
        log.info("리다이렉트 → {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                        name, value, maxAge));
    }
}
