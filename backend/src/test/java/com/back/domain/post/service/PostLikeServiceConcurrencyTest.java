package com.back.domain.post.service;

import com.back.domain.auth.domain.Provider;
import com.back.domain.auth.domain.Role;
import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.post.domain.Post;
import com.back.domain.post.repository.PostLikeRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.global.config.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class PostLikeServiceConcurrencyTest {

    @Autowired private PostLikeService postLikeService;
    @Autowired private PostRepository postRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private UserRepository userRepository;

    private Post post;
    private List<User> users;

    @BeforeEach
    void setUp() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // 동시에 좋아요를 누를 유저 100명 생성
        users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(userRepository.save(User.builder()
                    .email("user" + i + "@test.com")
                    .nickname("유저" + i)
                    .provider(Provider.GOOGLE)
                    .providerId("google-" + i)
                    .role(Role.USER)
                    .build()));
        }

        post = postRepository.save(Post.builder()
                .author(users.get(0))
                .title("동시성 테스트 게시글")
                .content("내용")
                .build());
    }

    @Test
    @DisplayName("100명이 동시에 좋아요 → likeCount가 100이어야 하지만 race condition으로 인해 더 작을 수 있음")
    void concurrentLike() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 전체 완료 대기

        for (int i = 0; i < threadCount; i++) {
            final Long userId = users.get(i).getId();
            executor.submit(() -> {
                try {
                    startLatch.await();  // 모든 스레드가 준비될 때까지 대기
                    postLikeService.toggleLike(userId, post.getId());
                } catch (Exception e) {
                    // 예외 발생 시 무시 (race condition으로 인한 예외 포함)
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 100개 스레드 동시 출발
        doneLatch.await();       // 전체 완료 대기
        executor.shutdown();

        Post result = postRepository.findById(post.getId()).orElseThrow();
        int actualLikeCount = result.getLikeCount();
        long actualPostLikeCount = postLikeRepository.count();

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("예상 likeCount: 100");
        System.out.println("실제 likeCount: " + actualLikeCount);
        System.out.println("PostLike 레코드 수: " + actualPostLikeCount);
        System.out.println("likeCount 불일치: " + (actualLikeCount != actualPostLikeCount));

        // likeCount와 PostLike 레코드 수가 일치해야 정상
        // race condition이 있으면 둘이 달라짐
        assertThat(actualLikeCount)
                .as("race condition 발생 시 likeCount가 PostLike 레코드 수와 다름")
                .isEqualTo((int) actualPostLikeCount);
    }
}