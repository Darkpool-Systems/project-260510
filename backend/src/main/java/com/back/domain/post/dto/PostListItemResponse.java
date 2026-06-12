package com.back.domain.post.dto;

import com.back.domain.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /posts 응답의 목록 항목 DTO
 */
@Getter
@Builder
public class PostListItemResponse {

    private final Long id;
    private final String title;
    private final String writer;
    private final int likeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;

    public static PostListItemResponse from(Post post) {
        return PostListItemResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .writer(post.getAuthor().getNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
