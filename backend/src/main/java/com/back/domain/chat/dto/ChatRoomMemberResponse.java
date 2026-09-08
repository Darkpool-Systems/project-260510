package com.back.domain.chat.dto;

import com.back.domain.chat.domain.ChatRoomMember;
import com.back.domain.chat.domain.ChatRoomMemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomMemberResponse {

    private final Long memberId;
    private final Long userId;
    private final String nickname;
    private final ChatRoomMemberStatus status;
    private final LocalDateTime requestedAt;

    public static ChatRoomMemberResponse from(ChatRoomMember member) {
        return ChatRoomMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .status(member.getStatus())
                .requestedAt(member.getCreatedAt())
                .build();
    }
}
