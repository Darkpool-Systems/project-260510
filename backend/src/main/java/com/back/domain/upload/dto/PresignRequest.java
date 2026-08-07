package com.back.domain.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignRequest {

    @NotBlank(message = "파일 이름은 필수입니다.")
    private String filename;

    @NotBlank(message = "파일 타입은 필수입니다.")
    private String contentType;

    @NotNull(message = "파일 크기는 필수입니다.")
    @Positive(message = "파일 크기는 0보다 커야 합니다.")
    private Long size;
}
