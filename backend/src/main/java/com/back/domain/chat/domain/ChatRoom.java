package com.back.domain.chat.domain;

import com.back.auth.domain.User;
import com.back.domain.post.domain.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 엔티티 - chat_rooms 테이블과 매핑
 * Post와 1:1 선택적 관계 (채팅방 없는 게시글 가능)
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String livekitRoomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatRoomStatus status;

    @Column(nullable = false)
    private int maxUsers;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(Post post, User owner, String title, String livekitRoomName, int maxUsers) {
        this.post = post;
        this.owner = owner;
        this.title = title;
        this.livekitRoomName = livekitRoomName;
        this.status = ChatRoomStatus.OPEN;
        this.maxUsers = maxUsers;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }
}
