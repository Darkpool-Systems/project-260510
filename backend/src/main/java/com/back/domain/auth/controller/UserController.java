package com.back.domain.auth.controller;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.auth.dto.NicknameUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자 정보 API - 인증된 사용자만 접근 가능
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        User user = getUser(authentication);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "role", user.getRole()
        ));
    }

    /**
     * 닉네임 변경
     * - 1~20자 제한
     * - 변경된 닉네임 즉시 반환
     */
    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(
            Authentication authentication,
            @Validated @RequestBody NicknameUpdateRequest request) {

        User user = getUser(authentication);
        user.updateNickname(request.getNickname());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "nickname", user.getNickname()
        ));
    }

    private User getUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
