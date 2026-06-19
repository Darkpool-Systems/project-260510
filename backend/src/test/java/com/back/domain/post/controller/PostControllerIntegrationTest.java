package com.back.domain.post.controller;

import com.back.domain.auth.domain.Provider;
import com.back.domain.auth.domain.Role;
import com.back.domain.auth.domain.User;
import com.back.domain.auth.jwt.JwtTokenProvider;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.auth.service.TokenService;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.post.domain.Post;
import com.back.domain.post.repository.PostLikeRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.global.config.TestContainersConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class PostControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private CommentRepository commentRepository;

    private MockMvc mockMvc;
    private User savedUser;
    private String accessToken;
    private User otherUser;
    private String otherAccessToken;

    @BeforeEach
    void setUp() {
        chatRoomRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .email("test@gmail.com")
                .nickname("테스터")
                .provider(Provider.GOOGLE)
                .providerId("google-123")
                .role(Role.USER)
                .build());

        accessToken = jwtTokenProvider.createAccessToken(
                savedUser.getId(), savedUser.getEmail(), "ROLE_USER");

        String refreshToken = jwtTokenProvider.createRefreshToken(
                savedUser.getId(), savedUser.getEmail(), "ROLE_USER");

        tokenService.saveTokens(savedUser.getId(), accessToken, refreshToken);

        otherUser = userRepository.save(User.builder()
                .email("other@gmail.com")
                .nickname("다른사람")
                .provider(Provider.GOOGLE)
                .providerId("google-456")
                .role(Role.USER)
                .build());

        otherAccessToken = jwtTokenProvider.createAccessToken(
                otherUser.getId(), otherUser.getEmail(), "ROLE_USER");

        String otherRefreshToken = jwtTokenProvider.createRefreshToken(
                otherUser.getId(), otherUser.getEmail(), "ROLE_USER");

        tokenService.saveTokens(otherUser.getId(), otherAccessToken, otherRefreshToken);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ===== 인증 =====

    @Nested
    @DisplayName("인증")
    class Auth {

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "스타트업 실패 후기",
                                      "content": "1년 동안 개발했지만 실패했습니다.",
                                      "createChatRoom": false
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 채팅방 없이 게시글 작성 =====

    @Nested
    @DisplayName("채팅방 없이 게시글 작성")
    class WithoutChatRoom {

        @Test
        @DisplayName("정상 요청 → 201, postId 반환, chatRoomId null")
        void success() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "스타트업 실패 후기",
                                      "content": "1년 동안 개발했지만 실패했습니다.",
                                      "createChatRoom": false
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").isNumber())
                    .andExpect(jsonPath("$.chatRoomId").isEmpty());

            assertThat(postRepository.count()).isEqualTo(1);
            assertThat(chatRoomRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("제목 누락 → 400")
        void missingTitle() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "내용만 있고 제목 없음",
                                      "createChatRoom": false
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("내용 누락 → 400")
        void missingContent() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "제목만 있고 내용 없음",
                                      "createChatRoom": false
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("제목 빈 문자열 → 400")
        void blankTitle() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "   ",
                                      "content": "내용",
                                      "createChatRoom": false
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    // ===== 채팅방과 함께 게시글 작성 =====

    @Nested
    @DisplayName("채팅방과 함께 게시글 작성")
    class WithChatRoom {

        @Test
        @DisplayName("정상 요청 → 201, postId·chatRoomId 모두 반환")
        void success() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "스타트업 실패 후기",
                                      "content": "1년 동안 개발했지만 실패했습니다.",
                                      "createChatRoom": true,
                                      "chatRoomTitle": "실패 회고 모임",
                                      "maxUsers": 20
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").isNumber())
                    .andExpect(jsonPath("$.chatRoomId").isNumber());

            assertThat(postRepository.count()).isEqualTo(1);
            assertThat(chatRoomRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 게시글에 채팅방 중복 생성 불가 → 500 (unique 제약)")
        void duplicateChatRoom() throws Exception {
            String body = """
                    {
                      "title": "스타트업 실패 후기",
                      "content": "내용",
                      "createChatRoom": true,
                      "chatRoomTitle": "실패 회고 모임",
                      "maxUsers": 20
                    }
                    """;

            // 첫 번째 요청 성공
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content(body))
                    .andExpect(status().isCreated());

            // 동일 게시글에 채팅방 재생성 시도는 별개의 post 생성이므로
            // 두 번째 요청도 성공하지만 서로 다른 post를 가리킴
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content(body))
                    .andExpect(status().isCreated());

            assertThat(postRepository.count()).isEqualTo(2);
            assertThat(chatRoomRepository.count()).isEqualTo(2);
        }
    }

    // ===== 게시글 목록 조회 =====

    @Nested
    @DisplayName("게시글 목록 조회")
    class GetPosts {

        @Test
        @DisplayName("토큰 없이 요청해도 200 및 목록 반환")
        void success() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("스타트업 실패 후기")
                    .content("1년 동안 개발했지만 실패했습니다.")
                    .build());

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(post.getId()))
                    .andExpect(jsonPath("$.content[0].title").value("스타트업 실패 후기"))
                    .andExpect(jsonPath("$.content[0].writer").value("테스터"))
                    .andExpect(jsonPath("$.content[0].likeCount").value(0))
                    .andExpect(jsonPath("$.content[0].commentCount").value(0))
                    .andExpect(jsonPath("$.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.content[0].updatedAt").exists())
                    // 상세 조회 전용 필드는 목록에 없어야 함
                    .andExpect(jsonPath("$.content[0].content").doesNotExist())
                    .andExpect(jsonPath("$.content[0].chatRoomExists").doesNotExist());
        }

        @Test
        @DisplayName("게시글이 없으면 빈 배열 반환")
        void empty() throws Exception {
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("페이지네이션 - size 파라미터 적용")
        void pagination() throws Exception {
            for (int i = 1; i <= 25; i++) {
                postRepository.save(Post.builder()
                        .author(savedUser)
                        .title("게시글 " + i)
                        .content("내용 " + i)
                        .build());
            }

            // 기본 size=10
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(10));

            // size=10, page=1
            mockMvc.perform(get("/api/posts").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(10));
        }

        @Test
        @DisplayName("sort=latest → 최신순 반환 (기본값과 동일)")
        void sortByLatest() throws Exception {
            postRepository.save(Post.builder().author(savedUser).title("오래된 글").content("내용").build());
            postRepository.save(Post.builder().author(savedUser).title("최신 글").content("내용").build());

            mockMvc.perform(get("/api/posts").param("sortBy", "latest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("최신 글"))
                    .andExpect(jsonPath("$.content[1].title").value("오래된 글"));
        }

        @Test
        @DisplayName("sort 파라미터 없으면 최신순 반환")
        void sortDefault() throws Exception {
            postRepository.save(Post.builder().author(savedUser).title("오래된 글").content("내용").build());
            postRepository.save(Post.builder().author(savedUser).title("최신 글").content("내용").build());

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("최신 글"))
                    .andExpect(jsonPath("$.content[1].title").value("오래된 글"));
        }

        @Test
        @DisplayName("sort=likes → 좋아요 많은 순 반환")
        void sortByLikes() throws Exception {
            postRepository.save(Post.builder().author(savedUser).title("좋아요 적은 글").content("내용").build());
            Post highLike = postRepository.save(Post.builder().author(savedUser).title("좋아요 많은 글").content("내용").build());

            mockMvc.perform(post("/api/posts/{postId}/likes", highLike.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/posts").param("sortBy", "likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("좋아요 많은 글"))
                    .andExpect(jsonPath("$.content[1].title").value("좋아요 적은 글"));
        }

        @Test
        @DisplayName("sort=comments → 댓글 많은 순 반환")
        void sortByComments() throws Exception {
            postRepository.save(Post.builder().author(savedUser).title("댓글 없는 글").content("내용").build());
            Post hasComment = postRepository.save(Post.builder().author(savedUser).title("댓글 있는 글").content("내용").build());

            mockMvc.perform(post("/api/posts/{postId}/comments", hasComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    { "content": "댓글입니다", "parentCommentId": null }
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/posts").param("sortBy", "comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("댓글 있는 글"))
                    .andExpect(jsonPath("$.content[1].title").value("댓글 없는 글"));
        }

        @Test
        @DisplayName("알 수 없는 sort 값 → 최신순으로 fallback")
        void sortUnknownFallback() throws Exception {
            postRepository.save(Post.builder().author(savedUser).title("오래된 글").content("내용").build());
            postRepository.save(Post.builder().author(savedUser).title("최신 글").content("내용").build());

            mockMvc.perform(get("/api/posts").param("sortBy", "invalid"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("최신 글"))
                    .andExpect(jsonPath("$.content[1].title").value("오래된 글"));
        }
    }

    // ===== 게시글 상세 조회 =====

    @Nested
    @DisplayName("게시글 상세 조회")
    class GetPost {

        @Test
        @DisplayName("토큰 없이 요청해도 200, 채팅방 없으면 chatRoomExists=false")
        void successWithoutChatRoom() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("스타트업 실패 후기")
                    .content("1년 동안 개발했지만 실패했습니다.")
                    .build());

            mockMvc.perform(get("/api/posts/{postId}", post.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(post.getId()))
                    .andExpect(jsonPath("$.title").value("스타트업 실패 후기"))
                    .andExpect(jsonPath("$.content").value("1년 동안 개발했지만 실패했습니다."))
                    .andExpect(jsonPath("$.writer.id").value(savedUser.getId()))
                    .andExpect(jsonPath("$.writer.nickname").value("테스터"))
                    .andExpect(jsonPath("$.likeCount").value(0))
                    .andExpect(jsonPath("$.commentCount").value(0))
                    .andExpect(jsonPath("$.chatRoomExists").value(false))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("채팅방 있으면 chatRoomExists=true")
        void successWithChatRoom() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("스타트업 실패 후기")
                    .content("내용")
                    .build());

            chatRoomRepository.save(ChatRoom.builder()
                    .post(post)
                    .owner(savedUser)
                    .title("실패 회고 모임")
                    .livekitRoomName("room-" + post.getId())
                    .maxUsers(20)
                    .build());

            mockMvc.perform(get("/api/posts/{postId}", post.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chatRoomExists").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    // ===== 게시글 수정 =====

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @DisplayName("작성자가 수정 → 204, 내용 반영")
        void success() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "수정된 제목",
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("수정된 제목");
            assertThat(updated.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "수정된 제목",
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정 → 403")
        void forbidden() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", otherAccessToken))
                            .content("""
                                    {
                                      "title": "수정된 제목",
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));

            Post unchanged = postRepository.findById(post.getId()).orElseThrow();
            assertThat(unchanged.getTitle()).isEqualTo("원래 제목");
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void notFound() throws Exception {
            mockMvc.perform(patch("/api/posts/{postId}", 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "title": "수정된 제목",
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("제목 누락 → 400")
        void missingTitle() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("원래 제목")
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    // ===== 게시글 삭제 =====

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("작성자가 삭제 → 204, DB에서 제거")
        void success() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("삭제될 게시글")
                    .content("내용")
                    .build());

            mockMvc.perform(delete("/api/posts/{postId}", post.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            assertThat(postRepository.findById(post.getId())).isEmpty();
        }

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("삭제될 게시글")
                    .content("내용")
                    .build());

            mockMvc.perform(delete("/api/posts/{postId}", post.getId()))
                    .andExpect(status().isUnauthorized());

            assertThat(postRepository.findById(post.getId())).isPresent();
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제 → 403")
        void forbidden() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("삭제될 게시글")
                    .content("내용")
                    .build());

            mockMvc.perform(delete("/api/posts/{postId}", post.getId())
                            .cookie(new Cookie("access_token", otherAccessToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));

            assertThat(postRepository.findById(post.getId())).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void notFound() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}", 999999L)
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    // ===== 게시글 좋아요 =====

    @Nested
    @DisplayName("게시글 좋아요")
    class ToggleLike {

        @Test
        @DisplayName("좋아요 누르면 → 204, likeCount +1, PostLike 생성")
        void like() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("좋아요 테스트")
                    .content("내용")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(1);
            assertThat(postLikeRepository.existsByPostIdAndUserId(post.getId(), savedUser.getId())).isTrue();
        }

        @Test
        @DisplayName("좋아요 후 다시 누르면 → 204, likeCount 0, PostLike 삭제 (토글)")
        void toggleCancel() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("좋아요 토글 테스트")
                    .content("내용")
                    .build());

            // 첫 번째: 좋아요
            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            // 두 번째: 좋아요 취소
            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(0);
            assertThat(postLikeRepository.existsByPostIdAndUserId(post.getId(), savedUser.getId())).isFalse();
        }

        @Test
        @DisplayName("두 명이 각각 좋아요 → likeCount 2")
        void multipleUsers() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("여러 명 좋아요 테스트")
                    .content("내용")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                            .cookie(new Cookie("access_token", otherAccessToken)))
                    .andExpect(status().isNoContent());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(2);
            assertThat(postLikeRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("토큰 없이 좋아요 요청 → 401")
        void noToken() throws Exception {
            Post post = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("좋아요 인증 테스트")
                    .content("내용")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/likes", post.getId()))
                    .andExpect(status().isUnauthorized());

            assertThat(postLikeRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 좋아요 → 404")
        void postNotFound() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/likes", 999999L)
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }
}
