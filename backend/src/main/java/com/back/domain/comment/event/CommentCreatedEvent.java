package com.back.domain.comment.event;

/**
 * 댓글/대댓글이 저장된 후 발행되는 이벤트
 * 알림 발송 등 부가 작업은 이 이벤트를 구독해서 트랜잭션 커밋 이후 처리
 */
public record CommentCreatedEvent(Long commentId) {
}
