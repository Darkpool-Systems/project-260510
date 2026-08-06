package com.back.global.ai;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * OpenAI Embeddings API 클라이언트
 * - 텍스트를 벡터로 변환하고 L2 정규화하여 반환
 */
@Slf4j
@Component
public class OpenAiEmbeddingClient {

    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";

    private final RestClient restClient;
    private final String model;

    public OpenAiEmbeddingClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.embedding-model:text-embedding-3-small}") String model
    ) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(EMBEDDING_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 텍스트를 임베딩 벡터로 변환 (L2 정규화 적용)
     */
    public float[] embed(String text) {
        EmbeddingResponse response;
        try {
            response = restClient.post()
                    .body(new EmbeddingRequest(model, text))
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (Exception e) {
            log.error("OpenAI 임베딩 생성 실패", e);
            throw new CustomException(ErrorCode.EMBEDDING_GENERATION_FAILED);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            log.error("OpenAI 임베딩 응답이 비어있음");
            throw new CustomException(ErrorCode.EMBEDDING_GENERATION_FAILED);
        }

        return normalize(response.data().get(0).embedding());
    }

    private float[] normalize(float[] vector) {
        double normSquared = 0;
        for (float v : vector) {
            normSquared += (double) v * v;
        }
        double norm = Math.sqrt(normSquared);
        if (norm == 0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    private record EmbeddingRequest(String model, String input) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(float[] embedding) {
    }
}
