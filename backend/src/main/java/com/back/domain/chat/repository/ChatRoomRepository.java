package com.back.domain.chat.repository;

import com.back.domain.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 게시글에 연결된 채팅방 존재 여부 확인
     */
    boolean existsByPostId(Long postId);

    /**
     * 게시글에 연결된 채팅방 조회 (참여자에게 roomId를 알려주기 위함)
     */
    Optional<ChatRoom> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}
