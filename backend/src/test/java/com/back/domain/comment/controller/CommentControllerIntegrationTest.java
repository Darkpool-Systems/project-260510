package com.back.domain.comment.controller;

import com.back.domain.auth.domain.Provider;
import com.back.domain.auth.domain.Role;
import com.back.domain.auth.domain.User;
import com.back.domain.auth.jwt.JwtTokenProvider;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.auth.service.TokenService;
import com.back.domain.comment.domain.Comment;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.post.domain.Post;
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
class CommentControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;

    private MockMvc mockMvc;
    private User savedUser;
    private String accessToken;
    private User otherUser;
    private String otherAccessToken;
    private Post post;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        chatRoomRepository.deleteAll();
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

        post = postRepository.save(Post.builder()
                .author(savedUser)
                .title("스타트업 실패 후기")
                .content("1년 동안 개발했지만 실패했습니다.")
                .build());

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ===== 댓글 작성 =====

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("최상위 댓글 작성 → 201, commentCount 증가")
        void success() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "공감합니다"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.commentId").isNumber());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getCommentCount()).isEqualTo(1);
            assertThat(commentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("대댓글 작성 → 201, parent 연결, commentCount 증가")
        void successReply() throws Exception {
            Comment parent = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("최상위 댓글")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", otherAccessToken))
                            .content("""
                                    {
                                      "content": "대댓글입니다",
                                      "parentId": %d
                                    }
                                    """.formatted(parent.getId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.commentId").isNumber());

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getCommentCount()).isEqualTo(1);
            assertThat(commentRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("대댓글에 답글 시도 → 400")
        void replyToReplyNotAllowed() throws Exception {
            Comment parent = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("최상위 댓글")
                    .build());

            Comment reply = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .parent(parent)
                    .content("대댓글")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "대대댓글 시도",
                                      "parentId": %d
                                    }
                                    """.formatted(reply.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMENT_REPLY_TO_REPLY_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("다른 게시글의 댓글을 parentId로 지정 → 400")
        void parentPostMismatch() throws Exception {
            Post otherPost = postRepository.save(Post.builder()
                    .author(savedUser)
                    .title("다른 게시글")
                    .content("내용")
                    .build());

            Comment parentInOtherPost = commentRepository.save(Comment.builder()
                    .post(otherPost)
                    .author(savedUser)
                    .content("다른 게시글의 댓글")
                    .build());

            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "댓글",
                                      "parentId": %d
                                    }
                                    """.formatted(parentInOtherPost.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMENT_PARENT_POST_MISMATCH"));
        }

        @Test
        @DisplayName("존재하지 않는 parentId → 404")
        void parentNotFound() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "댓글",
                                      "parentId": 999999
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_PARENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void postNotFound() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments", 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "댓글"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("내용 누락 → 400")
        void missingContent() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "댓글"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 댓글 목록 조회 =====

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("토큰 없이 요청해도 200, 최상위 댓글 + 대댓글 트리 반환")
        void success() throws Exception {
            Comment parent = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("최상위 댓글")
                    .build());

            commentRepository.save(Comment.builder()
                    .post(post)
                    .author(otherUser)
                    .parent(parent)
                    .content("대댓글")
                    .build());

            mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(parent.getId()))
                    .andExpect(jsonPath("$.content[0].content").value("최상위 댓글"))
                    .andExpect(jsonPath("$.content[0].writer.nickname").value("테스터"))
                    .andExpect(jsonPath("$.content[0].replies").isArray())
                    .andExpect(jsonPath("$.content[0].replies.length()").value(1))
                    .andExpect(jsonPath("$.content[0].replies[0].content").value("대댓글"))
                    .andExpect(jsonPath("$.content[0].replies[0].writer.nickname").value("다른사람"))
                    .andExpect(jsonPath("$.content[0].replies[0].replies").isArray())
                    .andExpect(jsonPath("$.content[0].replies[0].replies.length()").value(0));
        }

        @Test
        @DisplayName("댓글이 없으면 빈 배열")
        void empty() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void postNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    // ===== 댓글 수정 =====

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("작성자가 수정 → 204, 내용 반영")
        void success() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
            assertThat(updated.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정 → 403")
        void forbidden() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", otherAccessToken))
                            .content("""
                                    {
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));

            Comment unchanged = commentRepository.findById(comment.getId()).orElseThrow();
            assertThat(unchanged.getContent()).isEqualTo("원래 내용");
        }

        @Test
        @DisplayName("존재하지 않는 댓글 → 404")
        void notFound() throws Exception {
            mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", post.getId(), 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("내용 누락 → 400")
        void missingContent() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(new Cookie("access_token", accessToken))
                            .content("""
                                    {
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("원래 내용")
                    .build());

            mockMvc.perform(patch("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "수정된 내용"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 댓글 삭제 =====

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("대댓글 없는 최상위 댓글 삭제 → 204, commentCount 1 감소")
        void successTopLevel() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("삭제될 댓글")
                    .build());
            post.increaseCommentCount();
            postRepository.save(post);

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.findById(comment.getId())).isEmpty();
            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getCommentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("대댓글 있는 최상위 댓글 삭제 → 대댓글도 함께 삭제, commentCount (1+대댓글수) 감소")
        void successTopLevelWithReplies() throws Exception {
            Comment parent = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("최상위 댓글")
                    .build());
            Comment reply1 = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(otherUser)
                    .parent(parent)
                    .content("대댓글1")
                    .build());
            Comment reply2 = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(otherUser)
                    .parent(parent)
                    .content("대댓글2")
                    .build());

            post.increaseCommentCount();
            post.increaseCommentCount();
            post.increaseCommentCount();
            postRepository.save(post);

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), parent.getId())
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.findById(parent.getId())).isEmpty();
            assertThat(commentRepository.findById(reply1.getId())).isEmpty();
            assertThat(commentRepository.findById(reply2.getId())).isEmpty();
            assertThat(commentRepository.count()).isEqualTo(0);

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getCommentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("대댓글 삭제 → 204, commentCount 1 감소, 부모/형제는 유지")
        void successReply() throws Exception {
            Comment parent = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("최상위 댓글")
                    .build());
            Comment reply = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(otherUser)
                    .parent(parent)
                    .content("대댓글")
                    .build());

            post.increaseCommentCount();
            post.increaseCommentCount();
            postRepository.save(post);

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), reply.getId())
                            .cookie(new Cookie("access_token", otherAccessToken)))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.findById(reply.getId())).isEmpty();
            assertThat(commentRepository.findById(parent.getId())).isPresent();

            Post updated = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updated.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제 → 403")
        void forbidden() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("삭제될 댓글")
                    .build());

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                            .cookie(new Cookie("access_token", otherAccessToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));

            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 댓글 → 404")
        void notFound() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), 999999L)
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void noToken() throws Exception {
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .author(savedUser)
                    .content("삭제될 댓글")
                    .build());

            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId()))
                    .andExpect(status().isUnauthorized());

            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }
    }
}
