package com.back.domain.post.controller;

import com.back.domain.post.dto.*;
import com.back.domain.post.service.PostLikeService;
import com.back.domain.post.service.PostRecommendationService;
import com.back.domain.post.service.PostService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostRecommendationService postRecommendationService;

    /**
     * POST /posts
     * 게시글 작성 (채팅방 동시 생성 선택 가능)
     */
    @PostMapping
    public ResponseEntity<PostCreateResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        PostCreateResponse response = postService.createPost(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /posts?sortBy=latest|likes|comments&page=0&size=10
     * 게시글 목록 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPosts(
            @RequestParam(defaultValue = "latest") String sortBy,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PostListItemResponse> page = postService.getPosts(sortBy, pageable);
        return ResponseEntity.ok(Map.of("content", page.getContent()));
    }

    /**
     * GET /posts/{postId}
     * 게시글 상세 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long postId) {
        PostDetailResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /posts/{postId}/recommendations
     * 유사 게시글 Top-5 추천 (임베딩 코사인 유사도 기반)
     */
    @GetMapping("/{postId}/recommendations")
    public ResponseEntity<List<PostRecommendationItemResponse>> getRecommendations(@PathVariable Long postId) {
        List<PostRecommendationItemResponse> response = postRecommendationService.recommend(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /posts/{postId}
     * 게시글 수정 - 작성자만 가능
     */
    @PatchMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        PostDetailResponse response = postService.updatePost(userId, postId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /posts/{postId}
     * 게시글 삭제 - 작성자만 가능
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        postService.deletePost(userId, postId);

        return ResponseEntity.noContent().build();
    }

    /**
     * POST /posts/{postId}/likes
     * 좋아요 토글 - 좋아요/좋아요 취소
     */
    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        postLikeService.toggleLike(userId, postId);

        return ResponseEntity.noContent().build();
    }
}


