package com.videoai.video_intelligence.service;

import com.videoai.video_intelligence.dto.SearchResult;
import com.videoai.video_intelligence.entity.VideoChunk;
import com.videoai.video_intelligence.repository.VideoChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for performing semantic search over video chunks.
 * It converts the user's query into an embedding vector using the same
 * Vertex AI model as the ingestion pipeline, then queries pgvector for
 * the closest chunks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSearchService {

    private final EmbeddingModel embeddingModel;
    private final VideoChunkRepository videoChunkRepository;

    /**
     * Search for video chunks most similar to the provided query.
     *
     * @param query the free‑text search query supplied by the user
     * @param limit maximum number of results to return (default 10)
     * @return a list of {@link SearchResult} DTOs ordered by similarity
     */
    public List<SearchResult> search(String query, int limit) {
        log.debug("Generating embedding for search query: {}", query);
        // Generate embedding using the same model as ingestion pipeline
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
        float[] queryEmbedding = response.getResult().getOutput();
        log.debug("Query embedding dimension: {}", queryEmbedding.length);

        List<VideoChunk> chunks = videoChunkRepository.findSimilarChunks(queryEmbedding, limit);
        log.debug("Found {} similar video chunks", chunks.size());

        return chunks.stream()
                .map(chunk -> {
                    SearchResult result = new SearchResult();
                    result.setVideoId(chunk.getVideo().getId());
                    // Construct a simple URL – adjust as needed for your media server
                    result.setVideoUrl("/videos/" + chunk.getVideo().getId());
                    result.setStartTimestamp(chunk.getStartTimestamp());
                    result.setEndTimestamp(chunk.getEndTimestamp());
                    result.setTranscript(chunk.getTranscript());
                    // Placeholder confidence – could be derived from distance if desired
                    result.setConfidenceScore(0f);
                    return result;
                })
                .collect(Collectors.toList());
    }
}
