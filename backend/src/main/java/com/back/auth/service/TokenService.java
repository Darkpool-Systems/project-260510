package com.back.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 토큰 저장소
 * - 로그인 시 access/refresh 토큰을 Redis에 저장
 * - 요청 시 Redis에 저장된 토큰과 비교하여 유효성 검증 (즉시 폐기 가능)
 * - 로그아웃 시 Redis에서 삭제 → 즉시 무효화
 *
 * Key 구조:
 *   access:{userId}  → JWT 문자열 (TTL: 1시간)
 *   refresh:{userId} → JWT 문자열 (TTL: 7일)
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /** 로그인 성공 시 두 토큰을 Redis에 저장 */
    public void saveTokens(Long userId, String accessToken, String refreshToken) {
        redisTemplate.opsForValue().set(
                accessKey(userId), accessToken, Duration.ofMillis(accessTokenExpiry));
        redisTemplate.opsForValue().set(
                refreshKey(userId), refreshToken, Duration.ofMillis(refreshTokenExpiry));
    }

    /** Access Token 단독 저장 (재발급 시) */
    public void saveAccessToken(Long userId, String accessToken) {
        redisTemplate.opsForValue().set(
                accessKey(userId), accessToken, Duration.ofMillis(accessTokenExpiry));
    }

    /** Redis에 저장된 Access Token과 일치하는지 확인 */
    public boolean isAccessTokenValid(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(accessKey(userId));
        return token.equals(stored);
    }

    /** Redis에 저장된 Refresh Token과 일치하는지 확인 */
    public boolean isRefreshTokenValid(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(refreshKey(userId));
        return token.equals(stored);
    }

    /** 로그아웃 — 두 토큰 모두 삭제 → 즉시 무효화 */
    public void deleteTokens(Long userId) {
        redisTemplate.delete(accessKey(userId));
        redisTemplate.delete(refreshKey(userId));
    }

    private String accessKey(Long userId) {
        return "access:" + userId;
    }

    private String refreshKey(Long userId) {
        return "refresh:" + userId;
    }
}
