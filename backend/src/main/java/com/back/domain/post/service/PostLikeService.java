package com.back.domain.post.service;

import com.back.domain.post.repository.PostLikeRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private static final int MAX_RETRY = 5;

    private final PostLikeRepository postLikeRepository;
    private final PostLikeTransactionService postLikeTransactionService;

    /**
     * 좋아요 토글 with 낙관적 락 재시도
     * - 별도 빈(PostLikeTransactionService)을 호출해야 @Transactional 프록시가 정상 동작
     * - ObjectOptimisticLockingFailureException 발생 시 최대 3회 재시도
     */
    public void toggleLike(Long userId, Long postId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                postLikeTransactionService.toggleLike(userId, postId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("좋아요 낙관적 락 충돌 - postId: {}, userId: {}, 시도: {}/{}",
                        postId, userId, attempt, MAX_RETRY);
                if (attempt == MAX_RETRY) {
                    throw new CustomException(ErrorCode.LIKE_CONFLICT);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long postId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}