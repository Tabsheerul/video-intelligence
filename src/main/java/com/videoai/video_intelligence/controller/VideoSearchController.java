package com.videoai.video_intelligence.controller;

import com.videoai.video_intelligence.dto.SearchResponse;
import com.videoai.video_intelligence.dto.SearchResult;
import com.videoai.video_intelligence.service.VideoSearchService;
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
public class VideoSearchController {

    private final VideoSearchService videoSearchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam("q") String query,
                                                 @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<SearchResult> results = videoSearchService.search(query, limit);
        return ResponseEntity.ok(new SearchResponse(results));
    }
}
