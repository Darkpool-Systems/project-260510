package com.back.domain.post.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.service.ChatRoomService;
import com.back.domain.post.domain.Post;
import com.back.domain.post.dto.PostCreateRequest;
import com.back.domain.post.dto.PostCreateResponse;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    @Transactional
    public PostCreateResponse createPost(Long userId, PostCreateRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        postRepository.save(post);

        // 채팅방 생성 요청이 있을 때만 ChatRoomService에 위임
        Long chatRoomId = null;
        if (request.isCreateChatRoom()) {
            ChatRoom chatRoom = chatRoomService.createChatRoom(
                    post,
                    author,
                    request.getChatRoomTitle(),
                    request.getMaxUsers()
            );
            chatRoomId = chatRoom.getId();
        }

        return PostCreateResponse.builder()
                .postId(post.getId())
                .chatRoomId(chatRoomId)
                .build();
    }
}
