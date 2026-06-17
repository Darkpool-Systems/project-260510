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

@Service
@RequiredArgsConstructor
public class PostLikeService {

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

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long postId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}