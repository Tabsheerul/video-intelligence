package com.videoai.video_intelligence.repository;

import com.videoai.video_intelligence.entity.VideoChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoChunkRepository extends JpaRepository<VideoChunk, UUID> {

    List<VideoChunk> findByVideoId(UUID videoId);

    /**
     * Performs a semantic similarity search using pgvector's cosine distance operator (<->).
     * Order by distance ascending means closer vectors come first.
     * 
     * @param embedding The query vector (as a float array)
     * @param limit The maximum number of results to return
     * @return List of VideoChunks closest to the query
     */
    @Query(value = "SELECT * FROM video_chunks ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<VideoChunk> findSimilarChunks(@Param("embedding") String embedding, @Param("limit") int limit);
}
