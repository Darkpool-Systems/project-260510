package com.back.domain.comment.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.comment.domain.Comment;
import com.back.domain.comment.dto.CommentCreateRequest;
import com.back.domain.comment.dto.CommentCreateResponse;
import com.back.domain.comment.dto.CommentResponse;
import com.back.domain.comment.dto.CommentUpdateRequest;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.post.domain.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 댓글/대댓글 작성
     * - parentId가 없으면 최상위 댓글
     * - parentId가 있으면 해당 댓글이 같은 게시글에 속하는 최상위 댓글이어야 함 (대댓글의 대댓글 금지)
     */
    @Transactional
    public CommentCreateResponse createComment(Long userId, Long postId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_PARENT_NOT_FOUND));

            if (!parent.getPost().getId().equals(postId)) {
                throw new CustomException(ErrorCode.COMMENT_PARENT_POST_MISMATCH);
            }

            if (parent.isReply()) {
                throw new CustomException(ErrorCode.COMMENT_REPLY_TO_REPLY_NOT_ALLOWED);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .content(request.getContent())
                .build();

        commentRepository.save(comment);
        post.increaseCommentCount();

        return CommentCreateResponse.builder()
                .commentId(comment.getId())
                .build();
    }

    /**
     * 게시글의 댓글 목록 조회 (최상위 댓글 + 대댓글 트리)
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        List<Comment> comments = commentRepository.findAllByPostIdWithAuthor(postId);

        // parentId -> 대댓글 목록
        Map<Long, List<CommentResponse>> repliesByParentId = new LinkedHashMap<>();
        List<Comment> topLevelComments = new ArrayList<>();

        for (Comment comment : comments) {
            if (comment.isReply()) {
                repliesByParentId
                        .computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>())
                        .add(CommentResponse.from(comment, List.of()));
            } else {
                topLevelComments.add(comment);
            }
        }

        return topLevelComments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        repliesByParentId.getOrDefault(comment.getId(), List.of())
                ))
                .toList();
    }

    /**
     * 댓글 수정 - 작성자만 가능
     */
    @Transactional
    public void updateComment(Long userId, Long postId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getCommentOfPost(postId, commentId);

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }

        comment.update(request.getContent());
    }

    /**
     * 댓글 삭제 - 작성자만 가능
     * 최상위 댓글 삭제 시 대댓글도 함께 삭제하고, 댓글 수는 (본인 + 대댓글 수)만큼 감소
     */
    @Transactional
    public void deleteComment(Long userId, Long postId, Long commentId) {
        Comment comment = getCommentOfPost(postId, commentId);

        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }

        Post post = comment.getPost();

        if (comment.isReply()) {
            commentRepository.delete(comment);
            post.decreaseCommentCount();
            return;
        }

        List<Comment> all = commentRepository.findAllByPostIdWithAuthor(postId);
        long replyCount = all.stream()
                .filter(Comment::isReply)
                .filter(reply -> reply.getParent().getId().equals(commentId))
                .count();

        List<Comment> replies = all.stream()
                .filter(Comment::isReply)
                .filter(reply -> reply.getParent().getId().equals(commentId))
                .toList();

        commentRepository.deleteAll(replies);
        commentRepository.delete(comment);

        for (long i = 0; i < replyCount + 1; i++) {
            post.decreaseCommentCount();
        }
    }

    private Comment getCommentOfPost(Long postId, Long commentId) {
        Comment comment = commentRepository.findByIdWithAuthor(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        return comment;
    }
}
