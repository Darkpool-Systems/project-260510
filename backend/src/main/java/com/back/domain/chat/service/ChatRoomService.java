package com.back.domain.chat.service;

import com.back.auth.domain.User;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 게시글에 연결된 채팅방 생성
     * PostService에서 게시글 저장 후 호출
     * livekitRoomName은 UUID로 자동 생성
     */
    @Transactional
    public ChatRoom createChatRoom(Post post, User owner, String title, int maxUsers) {
        String livekitRoomName = "room-" + UUID.randomUUID();

        ChatRoom chatRoom = ChatRoom.builder()
                .post(post)
                .owner(owner)
                .title(title)
                .livekitRoomName(livekitRoomName)
                .maxUsers(maxUsers)
                .build();

        return chatRoomRepository.save(chatRoom);
    }
}
