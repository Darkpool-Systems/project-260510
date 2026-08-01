package com.back.domain.upload.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignResponse {

    private final String uploadUrl; // 브라우저가 R2로 PUT할 서명된 업로드 URL
    private final String fileUrl;   // 업로드 완료 후 본문에 삽입할 공개 조회 URL
    private final String key;       // R2에 저장되는 객체 키 (posts/{userId}/{uuid}.{ext})
    private final long expiresIn;   // uploadUrl 만료까지 남은 시간(초)
}
