package com.back.domain.post.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.chat.service.ChatRoomService;
import com.back.domain.post.domain.Post;
import com.back.domain.post.dto.PostCreateRequest;
import com.back.domain.post.dto.PostCreateResponse;
import com.back.domain.post.dto.PostDetailResponse;
import com.back.domain.post.dto.PostListItemResponse;
import com.back.domain.post.dto.PostUpdateRequest;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;
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

    /**
     * 게시글 목록 조회
     * @param sortBy latest(최신순) | likes(좋아요순) | comments(댓글수순), 기본값 latest
     */
    @Transactional(readOnly = true)
    public Page<PostListItemResponse> getPosts(String sortBy, Pageable pageable) {
        Page<Post> posts = switch (sortBy) {
            case "likes"    -> postRepository.findAllOrderByLikeCountDesc(pageable);
            case "comments" -> postRepository.findAllOrderByCommentCountDesc(pageable);
            default         -> postRepository.findAllOrderByCreatedAtDesc(pageable);
        };
        return posts.map(PostListItemResponse::from);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        boolean chatRoomExists = chatRoomRepository.existsByPostId(postId);

        return PostDetailResponse.of(post, chatRoomExists);
    }

    /**
     * 게시글 수정 - 작성자만 가능
     */
    @Transactional
    public void updatePost(Long userId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        post.update(request.getTitle(), request.getContent());
    }

    /**
     * 게시글 삭제 - 작성자만 가능
     */
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.POST_FORBIDDEN);
        }

        postRepository.delete(post);
    }
}
