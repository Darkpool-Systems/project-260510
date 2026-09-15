package com.back.domain.notification.listener;

import com.back.domain.comment.domain.Comment;
import com.back.domain.comment.event.CommentCreatedEvent;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 댓글/대댓글 생성에 대한 푸시 알림 발송
 * - 일반 댓글: 게시글 작성자에게 알림
 * - 대댓글: 부모 댓글 작성자에게 알림
 * 댓글 저장 트랜잭션이 커밋된 후, 별도 스레드에서 비동기로 실행되므로
 * 알림 발송이 느리거나 실패해도 댓글 작성 응답에는 영향을 주지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationListener {

    private final CommentRepository commentRepository;
    private final PushNotificationService pushNotificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        commentRepository.findByIdWithNotificationTargets(event.commentId())
                .ifPresentOrElse(this::notify, () ->
                        log.warn("알림 대상 댓글을 찾을 수 없음 - commentId={}", event.commentId())
                );
    }

    private void notify(Comment comment) {
        if (comment.isReply()) {
            notifyReply(comment);
        } else {
            notifyTopLevelComment(comment);
        }
    }

    private void notifyTopLevelComment(Comment comment) {
        Long postAuthorId = comment.getPost().getAuthor().getId();

        if (comment.isAuthor(postAuthorId)) {
            return; // 본인 게시글에 본인이 단 댓글
        }

        pushNotificationService.sendToUser(
                postAuthorId,
                comment.getAuthor().getNickname() + "님이 댓글을 남겼습니다",
                comment.getContent(),
                postUrl(comment)
        );
    }

    private void notifyReply(Comment comment) {
        Comment parent = comment.getParent();
        Long parentAuthorId = parent.getAuthor().getId();

        if (comment.isAuthor(parentAuthorId)) {
            return; // 본인 댓글에 본인이 단 대댓글
        }

        pushNotificationService.sendToUser(
                parentAuthorId,
                comment.getAuthor().getNickname() + "님이 답글을 남겼습니다",
                comment.getContent(),
                postUrl(comment)
        );
    }

    private String postUrl(Comment comment) {
        return frontendUrl + "/posts/" + comment.getPost().getId();
    }
}
