package com.videoai.video_intelligence.dto;

import java.util.UUID;

public class SearchResult {
    private UUID videoId;
    private String videoUrl;
    private double startTimestamp;
    private double endTimestamp;
    private String transcript;
    private float confidenceScore;

    public SearchResult() {}

    public SearchResult(UUID videoId, String videoUrl, double startTimestamp, double endTimestamp, String transcript, float confidenceScore) {
        this.videoId = videoId;
        this.videoUrl = videoUrl;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.transcript = transcript;
        this.confidenceScore = confidenceScore;
    }

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public double getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(double startTimestamp) { this.startTimestamp = startTimestamp; }
    public double getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(double endTimestamp) { this.endTimestamp = endTimestamp; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public float getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(float confidenceScore) { this.confidenceScore = confidenceScore; }
}
