package com.back.domain.post.repository;

import com.back.domain.post.domain.PostEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostEmbeddingRepository extends JpaRepository<PostEmbedding, Long> {

    Optional<PostEmbedding> findByPostId(Long postId);

    /**
     * 유사도 비교 대상: 특정 게시글을 제외한 전체 임베딩 (Post 함께 fetch join, N+1 방지)
     */
    @Query("select pe from PostEmbedding pe join fetch pe.post where pe.postId <> :postId")
    List<PostEmbedding> findAllExcludingPost(@Param("postId") Long postId);
}

