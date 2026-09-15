package com.back.domain.notification.controller;

import com.back.domain.notification.dto.PushSubscribeRequest;
import com.back.domain.notification.service.PushSubscriptionService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 프론트에서 발급받은 Web Push 구독 정보를 등록/해제하는 API
 * (Service Worker 연동은 프론트 작업 - 여기서는 서버 측 저장/해제만 담당)
 */
@RestController
@RequestMapping("/api/notifications/subscribe")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    /**
     * POST /api/notifications/subscribe
     * body: { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } }
     */
    @PostMapping
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody PushSubscribeRequest request,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        pushSubscriptionService.subscribe(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * DELETE /api/notifications/subscribe?endpoint=...
     */
    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(
            @RequestParam @NotBlank String endpoint,
            Authentication authentication
    ) {
        requireUserId(authentication);
        pushSubscriptionService.unsubscribe(endpoint);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
