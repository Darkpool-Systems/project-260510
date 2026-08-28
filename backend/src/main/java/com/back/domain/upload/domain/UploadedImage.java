package com.back.domain.upload.domain;

import com.back.domain.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * R2에 업로드된 이미지의 상태 추적 엔티티
 * - presign 발급 시 PENDING으로 생성
 * - 게시글 작성/수정 시 본문에 실제로 쓰인 key만 COMMITTED로 전환
 * - 스케줄러가 오래된 PENDING 건을 찾아 R2와 DB에서 함께 정리(고아 이미지 청소)
 */
@Entity
@Table(name = "uploaded_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, unique = true, length = 255)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User uploader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UploadedImage(String key, User uploader) {
        this.key = key;
        this.uploader = uploader;
        this.status = UploadStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void commit() {
        this.status = UploadStatus.COMMITTED;
    }

    public void uncommit() {
        this.status = UploadStatus.PENDING;
    }
}
