package com.back.domain.post.repository;

import com.back.domain.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 목록 조회 - 최신순
     */
    @Query(
            value = "SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p"
    )
    Page<Post> findAllOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 게시글 목록 조회 - 좋아요순
     */
    @Query(
            value = "SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.likeCount DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p"
    )
    Page<Post> findAllOrderByLikeCountDesc(Pageable pageable);

    /**
     * 게시글 목록 조회 - 댓글수순
     */
    @Query(
            value = "SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.commentCount DESC, p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p"
    )
    Page<Post> findAllOrderByCommentCountDesc(Pageable pageable);

    /**
     * 게시글 상세 조회 - author를 fetch join하여 N+1 방지
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
    Optional<Post> findByIdWithAuthor(@Param("id") Long id);
}
