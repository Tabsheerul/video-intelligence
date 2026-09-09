package com.videoai.video_intelligence.dto;

import java.util.List;

public class SearchResponse {
    private List<SearchResult> results;

    public SearchResponse() {}

    public SearchResponse(List<SearchResult> results) {
        this.results = results;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }
}
