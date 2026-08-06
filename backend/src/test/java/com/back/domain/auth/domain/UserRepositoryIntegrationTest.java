package com.back.domain.auth.domain;

import com.back.global.config.TestContainersConfig;
import com.back.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class UserRepositoryIntegrationTest {

    @Autowired private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser = userRepository.save(User.builder()
                .email("repo-test@gmail.com")
                .nickname("테스트유저")
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("신규 사용자 저장 → DB에 모든 필드 저장 확인")
    void saveUser() {
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("repo-test@gmail.com");
        assertThat(savedUser.getNickname()).isEqualTo("테스트유저");
        assertThat(savedUser.getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이메일로 사용자 조회 → 정상 반환")
    void findByEmail_exists() {
        Optional<User> result = userRepository.findByEmail("repo-test@gmail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("repo-test@gmail.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 → empty")
    void findByEmail_notFound() {
        Optional<User> result = userRepository.findByEmail("nobody@gmail.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 → true/false")
    void existsByEmail() {
        assertThat(userRepository.existsByEmail("repo-test@gmail.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@gmail.com")).isFalse();
    }

    @Test
    @DisplayName("닉네임 업데이트 → DB 반영 확인")
    void updateNickname() {
        savedUser.updateNickname("새이름");
        userRepository.save(savedUser);

        User updated = userRepository.findByEmail("repo-test@gmail.com").get();
        assertThat(updated.getNickname()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("기본 권한은 USER")
    void defaultRole_isUser() {
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getRole().getKey()).isEqualTo("ROLE_USER");
    }
}
