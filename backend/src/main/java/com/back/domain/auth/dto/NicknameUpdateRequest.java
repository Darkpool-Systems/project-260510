package com.back.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 닉네임 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 20, message = "닉네임은 1~20자 사이여야 합니다.")
    private String nickname;
}
