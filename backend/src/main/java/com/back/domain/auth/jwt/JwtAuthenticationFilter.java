package com.back.domain.auth.jwt;

import com.back.domain.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 인증 필터 - 매 요청마다 쿠키의 JWT를 검증
 *
 * 요청 → 쿠키에서 access_token 추출 → JWT 유효성 검증 → Redis 존재 여부 확인
 * → 둘 다 통과하면 SecurityContext에 인증 정보 세팅
 *
 * Redis 검증을 추가함으로써 로그아웃 시 즉시 토큰 무효화 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveTokenFromCookie(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);

            // JWT 서명 유효 + Redis에 저장된 토큰과 일치해야 인증 통과
            if (tokenService.isAccessTokenValid(userId, token)) {
                String email = jwtTokenProvider.getEmail(token);
                String role = jwtTokenProvider.getRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                email,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("인증 성공 - userId: {}, email: {}", userId, email);
            } else {
                log.debug("Redis에 토큰 없음 - 폐기된 토큰 (userId: {})", userId);
            }
        }

        filterChain.doFilter(request, response);
    }

    // 쿠키에서 access_token 값 추출
    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
