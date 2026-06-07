package com.back.domain.post.service;

import com.back.auth.domain.Provider;
import com.back.auth.domain.Role;
import com.back.auth.domain.User;
import com.back.auth.repository.UserRepository;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.service.ChatRoomService;
import com.back.domain.post.domain.Post;
import com.back.domain.post.dto.PostCreateRequest;
import com.back.domain.post.dto.PostCreateResponse;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@gmail.com")
                .nickname("테스터")
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .role(Role.USER)
                .build();
    }

    // ===== 채팅방 없이 게시글 작성 =====

    @Nested
    @DisplayName("채팅방 없이 게시글 작성")
    class CreatePostWithoutChatRoom {

        @Test
        @DisplayName("정상 요청 → postId 반환, chatRoomId는 null")
        void success() {
            // given
            PostCreateRequest request = buildRequest("실패 후기", "내용입니다.", false, null, null);

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            PostCreateResponse response = postService.createPost(1L, request);

            // then
            assertThat(response.getChatRoomId()).isNull();
            then(postRepository).should().save(any(Post.class));
            then(chatRoomService).should(never()).createChatRoom(any(), any(), any(), any(int.class));
        }

        @Test
        @DisplayName("존재하지 않는 userId → USER_NOT_FOUND 예외")
        void userNotFound() {
            // given
            PostCreateRequest request = buildRequest("제목", "내용", false, null, null);
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.createPost(999L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));

            then(postRepository).should(never()).save(any());
        }
    }

    // ===== 채팅방과 함께 게시글 작성 =====

    @Nested
    @DisplayName("채팅방과 함께 게시글 작성")
    class CreatePostWithChatRoom {

        @Test
        @DisplayName("정상 요청 → postId, chatRoomId 모두 반환")
        void success() {
            // given
            PostCreateRequest request = buildRequest("실패 후기", "내용입니다.", true, "실패 회고 모임", 20);

            ChatRoom mockChatRoom = ChatRoom.builder()
                    .post(any())
                    .owner(mockUser)
                    .title("실패 회고 모임")
                    .livekitRoomName("room-uuid-1234")
                    .maxUsers(20)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));
            given(chatRoomService.createChatRoom(any(Post.class), eq(mockUser), eq("실패 회고 모임"), eq(20)))
                    .willReturn(mockChatRoom);

            // when
            PostCreateResponse response = postService.createPost(1L, request);

            // then
            then(postRepository).should().save(any(Post.class));
            then(chatRoomService).should().createChatRoom(any(Post.class), eq(mockUser), eq("실패 회고 모임"), eq(20));
        }

        @Test
        @DisplayName("존재하지 않는 userId → USER_NOT_FOUND 예외, 채팅방 생성 안 함")
        void userNotFound() {
            // given
            PostCreateRequest request = buildRequest("제목", "내용", true, "채팅방", 10);
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.createPost(999L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));

            then(chatRoomService).should(never()).createChatRoom(any(), any(), any(), any(int.class));
        }
    }

    // ===== 헬퍼 =====

    private PostCreateRequest buildRequest(
            String title, String content,
            boolean createChatRoom, String chatRoomTitle, Integer maxUsers
    ) {
        try {
            PostCreateRequest req = new PostCreateRequest();
            setField(req, "title", title);
            setField(req, "content", content);
            setField(req, "createChatRoom", createChatRoom);
            setField(req, "chatRoomTitle", chatRoomTitle);
            setField(req, "maxUsers", maxUsers);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
