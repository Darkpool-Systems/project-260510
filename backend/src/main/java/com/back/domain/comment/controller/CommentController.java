package com.back.domain.comment.controller;

import com.back.domain.comment.dto.CommentCreateRequest;
import com.back.domain.comment.dto.CommentCreateResponse;
import com.back.domain.comment.dto.CommentResponse;
import com.back.domain.comment.dto.CommentUpdateRequest;
import com.back.domain.comment.service.CommentService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/posts/{postId}/comments
     * 댓글/대댓글 작성
     */
    @PostMapping
    public ResponseEntity<CommentCreateResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        CommentCreateResponse response = commentService.createComment(userId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/posts/{postId}/comments
     * 댓글 목록 조회 (대댓글 포함, 트리 구조)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getComments(postId);
        return ResponseEntity.ok(Map.of("content", comments));
    }

    /**
     * PATCH /api/posts/{postId}/comments/{commentId}
     * 댓글 수정 - 작성자만 가능
     */
    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        commentService.updateComment(userId, postId, commentId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/posts/{postId}/comments/{commentId}
     * 댓글 삭제 - 작성자만 가능 (최상위 댓글 삭제 시 대댓글도 함께 삭제)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        commentService.deleteComment(userId, postId, commentId);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
