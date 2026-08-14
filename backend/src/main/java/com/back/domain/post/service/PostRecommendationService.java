package com.back.domain.post.service;

import com.back.domain.post.domain.PostEmbedding;
import com.back.domain.post.dto.PostRecommendationItemResponse;
import com.back.domain.post.repository.PostEmbeddingRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 게시글 임베딩 기반 유사 게시글 추천
 * - 대상 게시글의 벡터와 나머지 게시글 벡터를 내적(코사인 유사도, 벡터가 L2 정규화되어 있으므로 내적=코사인)으로 비교
 * - MVP 기준 O(n) 전수 스캔
 */
@Service
@RequiredArgsConstructor
public class PostRecommendationService {

    private static final int TOP_K = 5;

    private final PostEmbeddingRepository postEmbeddingRepository;

    @Transactional(readOnly = true)
    public List<PostRecommendationItemResponse> recommend(Long postId) {
        PostEmbedding target = postEmbeddingRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_EMBEDDING_NOT_FOUND));

        List<PostEmbedding> candidates = postEmbeddingRepository.findAllExcludingPost(postId);

        List<PostEmbedding> topCandidates = candidates.stream()
                .sorted(Comparator.comparingDouble(
                        (PostEmbedding candidate) -> similarity(target.getEmbedding(), candidate.getEmbedding())
                ).reversed())
                .limit(TOP_K)
                .toList();

        return IntStream.range(0, topCandidates.size())
                .mapToObj(i -> toItem(topCandidates.get(i), i + 1))
                .toList();
    }

    private double similarity(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private PostRecommendationItemResponse toItem(PostEmbedding embedding, int rank) {
        return PostRecommendationItemResponse.builder()
                .rank(rank)
                .postId(embedding.getPostId())
                .title(embedding.getPost().getTitle())
                .content(embedding.getPost().getContent())
                .build();
    }
}

