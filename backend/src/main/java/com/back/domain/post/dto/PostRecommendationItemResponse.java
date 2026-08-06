package com.back.domain.post.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * GET /posts/{postId}/recommendations 응답의 개별 추천 항목 DTO
 */
@Getter
@Builder
public class PostRecommendationItemResponse {

    private final int rank;
    private final String title;
    private final String content;
}
