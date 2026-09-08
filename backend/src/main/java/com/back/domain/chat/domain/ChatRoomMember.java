package com.back.domain.chat.domain;

import com.back.domain.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 참여 요청/멤버십 엔티티 - chat_room_members 테이블과 매핑
 * 참여 요청 시 PENDING으로 생성, 방장이 수락/거절
 */
@Entity
@Table(
        name = "chat_room_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomMemberStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatRoomMember(ChatRoom room, User user, ChatRoomMemberRole role) {
        this.room = room;
        this.user = user;
        this.role = role;
        this.status = ChatRoomMemberStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = ChatRoomMemberStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ChatRoomMemberStatus.REJECTED;
    }

    public boolean isPending() {
        return this.status == ChatRoomMemberStatus.PENDING;
    }
}
