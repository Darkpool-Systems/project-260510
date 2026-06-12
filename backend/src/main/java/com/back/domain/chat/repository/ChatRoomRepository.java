package com.back.domain.chat.repository;

import com.back.domain.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 게시글에 연결된 채팅방 존재 여부 확인
     */
    boolean existsByPostId(Long postId);
}
