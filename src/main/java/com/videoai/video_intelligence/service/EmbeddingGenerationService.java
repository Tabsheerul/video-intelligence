package com.videoai.video_intelligence.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates vector embeddings from transcript text using Spring AI's EmbeddingModel.
 *
 * The underlying model is configured in application.properties as text-embedding-004
 * from Google Vertex AI, which produces 768-dimensional vectors by default.
 *
 * These vectors are stored in pgvector and used for semantic similarity search.
 */
@Slf4j
@Service
public class EmbeddingGenerationService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingGenerationService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates a vector embedding for a single text string.
     *
     * @param text the transcript text to vectorize
     * @return a float array representing the embedding vector
     */
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text: {}...", text.substring(0, Math.min(text.length(), 80)));

        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        float[] embedding = response.getResult().getOutput();

        log.debug("Generated embedding with {} dimensions", embedding.length);
        return embedding;
    }
}
