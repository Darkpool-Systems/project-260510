package com.back.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "게시글에 대한 권한이 없습니다."),
    POST_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),
    LIKE_CONFLICT(HttpStatus.CONFLICT, "요청이 많아 처리에 실패했습니다. 다시 시도해주세요."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글에 대한 권한이 없습니다."),
    COMMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    COMMENT_REPLY_TO_REPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 작성할 수 없습니다."),
    COMMENT_PARENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "부모 댓글이 해당 게시글에 속하지 않습니다."),

    // ChatRoom
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_FULL(HttpStatus.BAD_REQUEST, "채팅방 인원이 초과되었습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
