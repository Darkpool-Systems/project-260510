package com.back.domain.post.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.post.domain.Post;
import com.back.domain.post.domain.PostLike;
import com.back.domain.post.repository.PostLikeRepository;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 낙관적 락 재시도를 위해 트랜잭션 로직을 별도 빈으로 분리
 * - Spring @Transactional은 프록시 기반이라 같은 클래스 내부 호출 시 동작 안 함
 * - PostLikeService → PostLikeTransactionService 호출 구조로 프록시 정상 동작 보장
 */
@Service
@RequiredArgsConstructor
public class PostLikeTransactionService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public void toggleLike(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        postLikeRepository.findByPostIdAndUserId(postId, userId)
                .ifPresentOrElse(
                        like -> {
                            postLikeRepository.delete(like);
                            post.decreaseLikeCount();
                        },
                        () -> {
                            postLikeRepository.save(
                                    PostLike.builder().post(post).user(user).build()
                            );
                            post.increaseLikeCount();
                        }
                );
    }
}