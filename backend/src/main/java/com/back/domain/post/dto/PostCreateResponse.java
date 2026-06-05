package com.back.domain.post.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostCreateResponse {

    private final Long postId;
    private final Long chatRoomId; // 채팅방 미생성 시 null
}
