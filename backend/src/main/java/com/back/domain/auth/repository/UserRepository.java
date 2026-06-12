package com.back.domain.auth.repository;

import com.back.domain.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User JPA Repository
 * - JpaRepository 상속으로 기본 CRUD 자동 생성
 * - 메서드명 규칙에 따라 Spring Data JPA가 쿼리 자동 생성
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 이메일로 사용자 조회 */
    Optional<User> findByEmail(String email);

    /** 이메일 존재 여부 확인 (신규 사용자 판단용) */
    boolean existsByEmail(String email);

    /** 닉네임 존재 여부 확인 (중복 조회용) */
    boolean existsByNickname(String nickname);
}
