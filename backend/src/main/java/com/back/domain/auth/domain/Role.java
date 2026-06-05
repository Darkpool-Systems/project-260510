package com.back.domain.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한 - Spring Security는 "ROLE_" 접두사를 요구
 * hasRole("ADMIN") → 내부적으로 "ROLE_ADMIN"과 비교
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;
}
