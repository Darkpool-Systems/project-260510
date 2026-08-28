package com.back.domain.post.service;

import com.back.domain.post.domain.Post;
import com.back.domain.post.domain.PostEmbedding;
import com.back.domain.post.repository.PostEmbeddingRepository;
import com.back.global.ai.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 임베딩 생성/갱신 담당
 * - 게시글 생성/수정 시 제목+내용을 임베딩하여 post_embeddings 테이블에 반영
 */
@Service
@RequiredArgsConstructor
public class PostEmbeddingService {

    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final PostEmbeddingRepository postEmbeddingRepository;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String model;

    @Transactional
    public void saveOrUpdateEmbedding(Post post) {
        float[] vector = openAiEmbeddingClient.embed(buildEmbeddingText(post));

        postEmbeddingRepository.findByPostId(post.getId())
                .ifPresentOrElse(
                        existing -> existing.updateEmbedding(vector, model),
                        () -> postEmbeddingRepository.save(new PostEmbedding(post, vector, model))
                );
    }

    @Transactional
    public void deleteEmbedding(Post post) {
        postEmbeddingRepository.deleteById(post.getId());
    }

    private String buildEmbeddingText(Post post) {
        return post.getTitle() + "\n\n" + post.getContent();
    }
}
