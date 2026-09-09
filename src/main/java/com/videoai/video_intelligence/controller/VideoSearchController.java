package com.videoai.video_intelligence.controller;

import com.videoai.video_intelligence.dto.SearchResponse;
import com.videoai.video_intelligence.dto.SearchResult;
import com.videoai.video_intelligence.service.VideoSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Semantic Search", description = "Low-latency semantic search over video transcripts using pgvector")
public class VideoSearchController {

    private final VideoSearchService videoSearchService;

    @Operation(
            summary = "Search videos by semantic query",
            description = "Converts a free-text query into a vector embedding using Google Vertex AI, "
                    + "then performs a cosine-similarity search against all video chunk embeddings stored "
                    + "in pgvector. Returns the most relevant video segments with timestamps."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results returned successfully"),
            @ApiResponse(responseCode = "400", description = "Missing query parameter"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "The search query text", required = true, example = "red shoes")
            @RequestParam("q") String query,
            @Parameter(description = "Maximum number of results to return", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<SearchResult> results = videoSearchService.search(query, limit);
        return ResponseEntity.ok(new SearchResponse(results));
    }
}
