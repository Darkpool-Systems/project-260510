package com.back.domain.chat.repository;

import com.back.domain.chat.domain.ChatRoomMember;
import com.back.domain.chat.domain.ChatRoomMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserIdAndStatus(Long roomId, Long userId, ChatRoomMemberStatus status);

    Optional<ChatRoomMember> findByIdAndRoomId(Long id, Long roomId);

    List<ChatRoomMember> findAllByRoomIdAndStatus(Long roomId, ChatRoomMemberStatus status);

    long countByRoomIdAndStatus(Long roomId, ChatRoomMemberStatus status);
}
