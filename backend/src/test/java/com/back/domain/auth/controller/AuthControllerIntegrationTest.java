package com.back.domain.auth.controller;

import com.back.global.config.TestContainersConfig;
import com.back.domain.auth.jwt.JwtTokenProvider;
import com.back.domain.auth.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestContainersConfig.class)
class AuthControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenService tokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ===== GET /api/auth/status =====

    @Test
    @DisplayName("인증 상태 확인 - 유효한 쿠키 + Redis → 200 + authenticated:true")
    void status_withValidCookie() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "test@gmail.com", "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "test@gmail.com", "ROLE_USER");
        tokenService.saveTokens(1L, accessToken, refreshToken);

        mockMvc.perform(get("/api/auth/status")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @DisplayName("인증 상태 확인 - 쿠키 없음 → 401 + authenticated:false")
    void status_withoutCookie() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("인증 상태 확인 - 잘못된 토큰 → 401 + authenticated:false")
    void status_withInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                        .cookie(new Cookie("access_token", "invalid.token.value")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("인증 상태 확인 - JWT 유효하지만 Redis에 없음 → 401")
    void status_validJwtButNotInRedis() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(99L, "nobody@gmail.com", "ROLE_USER");
        // Redis에 저장하지 않음

        mockMvc.perform(get("/api/auth/status")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    // ===== POST /api/auth/refresh =====

    @Test
    @DisplayName("토큰 재발급 - 유효한 refresh_token + Redis → 200 + 새 access_token 쿠키")
    void refresh_withValidRefreshToken() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "test@gmail.com", "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "test@gmail.com", "ROLE_USER");
        tokenService.saveTokens(1L, accessToken, refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed"))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(containsString("access_token"))));
    }

    @Test
    @DisplayName("토큰 재발급 - refresh_token 없음 → 401")
    void refresh_withoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token expired"));
    }

    @Test
    @DisplayName("토큰 재발급 - 잘못된 refresh_token → 401")
    void refresh_withInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "invalid.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 - JWT 유효하지만 Redis에 없음 → 401")
    void refresh_validJwtButNotInRedis() throws Exception {
        String refreshToken = jwtTokenProvider.createRefreshToken(99L, "nobody@gmail.com", "ROLE_USER");
        // Redis에 저장하지 않음

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    // ===== POST /api/auth/logout =====

    @Test
    @DisplayName("로그아웃 → 200 + 쿠키 삭제 + Redis 삭제")
    void logout() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "test@gmail.com", "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "test@gmail.com", "ROLE_USER");
        tokenService.saveTokens(1L, accessToken, refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(containsString("Max-Age=0"))));

        // 로그아웃 후 Redis에서 삭제되었으므로 status → 401
        mockMvc.perform(get("/api/auth/status")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isUnauthorized());
    }
}
