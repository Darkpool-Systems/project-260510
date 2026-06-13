package com.back.domain.comment.dto;

import com.back.domain.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/posts/{postId}/comments 응답의 댓글 항목
 * 최상위 댓글은 replies에 대댓글 목록을 가짐 (대댓글은 항상 빈 리스트)
 */
@Getter
@Builder
public class CommentResponse {

    private final Long id;
    private final String content;
    private final Writer writer;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<CommentResponse> replies;

    public static CommentResponse from(Comment comment, List<CommentResponse> replies) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(Writer.from(comment.getAuthor()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }

    public record Writer(Long id, String nickname) {
        public static Writer from(com.back.domain.auth.domain.User user) {
            return new Writer(user.getId(), user.getNickname());
        }
    }
}
