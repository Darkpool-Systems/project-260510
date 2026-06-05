package com.back.domain.auth.controller;

import com.back.domain.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class AuthControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;

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
    @DisplayName("인증 상태 확인 - 유효한 쿠키 → 200 + authenticated:true")
    void status_withValidCookie() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "test@gmail.com", "ROLE_USER");

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

    // ===== POST /api/auth/refresh =====

    @Test
    @DisplayName("토큰 재발급 - 유효한 refresh_token → 200 + 새 access_token 쿠키")
    void refresh_withValidRefreshToken() throws Exception {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "test@gmail.com", "ROLE_USER");

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

    // ===== POST /api/auth/logout =====

    @Test
    @DisplayName("로그아웃 → 200 + 쿠키 Max-Age=0 으로 삭제")
    void logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(containsString("Max-Age=0"))));
    }

    @Test
    @DisplayName("로그아웃 후 status 확인 → 401")
    void logout_thenStatusUnauthorized() throws Exception {
        // 로그아웃
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        // 쿠키 없이 status 확인
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
