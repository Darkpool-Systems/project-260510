package com.back.domain.auth.controller;

import com.back.domain.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/**
 * 인증 API 컨트롤러
 * - POST /api/auth/refresh : Access Token 재발급
 * - POST /api/auth/logout  : 로그아웃 (쿠키 삭제)
 * - GET  /api/auth/status  : 로그인 여부 확인
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    // Access Token 재발급 - refresh_token 쿠키로 새 access_token 발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveTokenFromCookie(request, "refresh_token");

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token expired"));
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        String role = jwtTokenProvider.getRole(refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, email, role);
        addCookie(response, "access_token", newAccessToken, (int) (accessTokenExpiry / 1000));

        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }

    // 로그아웃 - HttpOnly 쿠키는 JS에서 삭제 불가하므로 서버에서 Max-Age=0으로 삭제
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // 인증 상태 확인 - HttpOnly 쿠키는 JS에서 읽을 수 없으므로 이 API로 확인
    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        String accessToken = resolveTokenFromCookie(request, "access_token");

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        }

        return ResponseEntity.status(401).body(Map.of("authenticated", false));
    }

    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                        name, value, maxAge));
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie",
                String.format("%s=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax", name));
    }
}
