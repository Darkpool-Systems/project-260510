package com.back.domain.post.dto;

import com.back.domain.auth.domain.User;
import com.back.domain.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /posts/{postId} 응답 DTO
 */
@Getter
@Builder
public class PostDetailResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final Writer writer;
    private final int likeCount;
    private final int commentCount;
    private final boolean chatRoomExists;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailResponse of(Post post, boolean chatRoomExists) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(Writer.from(post.getAuthor()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .chatRoomExists(chatRoomExists)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * 게시글 상세 조회 응답의 writer 필드
     */
    public record Writer(Long id, String nickname) {
        public static Writer from(User user) {
            return new Writer(user.getId(), user.getNickname());
        }
    }
}
