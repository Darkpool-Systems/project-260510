package com.back.domain.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브라우저 PushSubscription.toJSON() 형태를 그대로 받는 요청 DTO
 * { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } }
 */
@Getter
@NoArgsConstructor
public class PushSubscribeRequest {

    @NotBlank(message = "endpoint는 필수입니다.")
    private String endpoint;

    @NotNull(message = "keys는 필수입니다.")
    @Valid
    private Keys keys;

    @Getter
    @NoArgsConstructor
    public static class Keys {

        @NotBlank(message = "p256dh는 필수입니다.")
        private String p256dh;

        @NotBlank(message = "auth는 필수입니다.")
        private String auth;
    }
}
