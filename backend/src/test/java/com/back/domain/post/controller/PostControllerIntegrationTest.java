package com.back.domain.post.controller;

import com.back.domain.auth.domain.Provider;
import com.back.domain.auth.domain.Role;
import com.back.domain.auth.domain.User;
import com.back.domain.auth.jwt.JwtTokenProvider;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.auth.service.TokenService;
import com.back.domain.chat.domain.ChatRoom;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired private ChatRoomRepository chatRoomRepository;

    private MockMvc mockMvc;
    private User savedUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
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

            // 기본 size=5
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5));

            // size=10, page=1
            mockMvc.perform(get("/api/posts").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(10));
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
}
