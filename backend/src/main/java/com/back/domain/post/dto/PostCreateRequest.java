package com.back.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    // 채팅방 생성 여부 (false면 아래 필드 무시)
    private boolean createChatRoom;

    @Size(max = 100, message = "채팅방 제목은 100자 이하여야 합니다.")
    private String chatRoomTitle;

    private Integer maxUsers;
}
