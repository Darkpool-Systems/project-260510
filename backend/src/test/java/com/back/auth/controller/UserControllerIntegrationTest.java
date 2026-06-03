package com.back.auth.controller;

import com.back.TestContainersConfig;
import com.back.auth.domain.Provider;
import com.back.auth.domain.Role;
import com.back.auth.domain.User;
import com.back.auth.repository.UserRepository;
import com.back.auth.jwt.JwtTokenProvider;
import com.back.auth.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestContainersConfig.class)
class UserControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;

    private MockMvc mockMvc;
    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .email("test@gmail.com")
                .nickname("테스트유저")
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .role(Role.USER)
                .build());

        accessToken = jwtTokenProvider.createAccessToken(
                testUser.getId(), testUser.getEmail(), testUser.getRole().getKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                testUser.getId(), testUser.getEmail(), testUser.getRole().getKey());

        // Redis에 토큰 저장 — 인증 필터가 Redis 검증을 하므로 필수
        tokenService.saveTokens(testUser.getId(), accessToken, refreshToken);
    }

    // ===== GET /api/user/me =====

    @Test
    @DisplayName("내 정보 조회 - 인증된 사용자 → 200 + 유저 정보")
    void getMyInfo_authenticated() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("내 정보 조회 - 쿠키 없음 → 인증 거부")
    void getMyInfo_withoutCookie() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    // ===== PATCH /api/user/nickname =====

    @Test
    @DisplayName("닉네임 변경 - 정상 요청 → 200 + 변경된 닉네임 반환")
    void updateNickname_success() throws Exception {
        mockMvc.perform(patch("/api/user/nickname")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"새닉네임\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"));

        // DB에도 실제로 반영됐는지 확인
        User updated = userRepository.findById(testUser.getId()).get();
        assertThat(updated.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("닉네임 변경 - 빈 값 → 400")
    void updateNickname_blank() throws Exception {
        mockMvc.perform(patch("/api/user/nickname")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 변경 - 20자 초과 → 400")
    void updateNickname_tooLong() throws Exception {
        mockMvc.perform(patch("/api/user/nickname")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"이건너무긴닉네임이라서반드시실패해야합니다\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 변경 - 쿠키 없음 → 인증 거부")
    void updateNickname_withoutCookie() throws Exception {
        mockMvc.perform(patch("/api/user/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"새닉네임\"}"))
                .andExpect(status().isUnauthorized());
    }
}