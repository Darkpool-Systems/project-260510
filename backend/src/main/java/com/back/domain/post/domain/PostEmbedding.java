package com.back.domain.post.domain;

import com.back.global.converter.FloatArrayConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 임베딩 엔티티 - post_embeddings 테이블과 매핑
 * - Post와 1:1, post_id를 PK이자 FK로 공유 (@MapsId)
 * - 목록/상세 조회 등 일상적인 쿼리에 대용량 벡터가 딸려오지 않도록 Post와 분리된 테이블로 관리
 * - post 하드 삭제 시 DB의 ON DELETE CASCADE로 함께 삭제됨 (애플리케이션 코드에서 별도 처리 불필요)
 */
@Entity
@Table(name = "post_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEmbedding {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding", columnDefinition = "TEXT", nullable = false)
    private float[] embedding;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PostEmbedding(Post post, float[] embedding, String model) {
        this.post = post;
        this.embedding = embedding;
        this.model = model;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmbedding(float[] embedding, String model) {
        this.embedding = embedding;
        this.model = model;
        this.updatedAt = LocalDateTime.now();
    }
}
