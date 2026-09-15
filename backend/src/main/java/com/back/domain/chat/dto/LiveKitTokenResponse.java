package com.back.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiveKitTokenResponse {
    private final String token;
    private final String url;
}
