package com.back.domain.comment.repository;

import com.back.domain.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글의 댓글 전체 조회 (author fetch join, 작성순)
     * 최상위 댓글과 대댓글을 모두 가져온 뒤 서비스 계층에서 트리로 묶음
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostIdWithAuthor(@Param("postId") Long postId);

    /**
     * id + author fetch join 단건 조회
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.id = :id")
    Optional<Comment> findByIdWithAuthor(@Param("id") Long id);
}
