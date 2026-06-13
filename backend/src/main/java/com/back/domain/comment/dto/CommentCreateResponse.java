package com.back.domain.comment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentCreateResponse {

    private final Long commentId;
}
