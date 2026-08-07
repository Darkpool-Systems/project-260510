package com.back.domain.upload.domain;

/**
 * 업로드된 이미지의 상태
 * - PENDING: presign 발급 시 생성, 아직 게시글에 커밋되지 않음
 * - COMMITTED: 게시글 작성/수정 완료 시, 본문에 실제로 쓰인 이미지로 확정됨
 */
public enum UploadStatus {
    PENDING,
    COMMITTED
}
