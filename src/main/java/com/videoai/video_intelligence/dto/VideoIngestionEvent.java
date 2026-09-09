package com.videoai.video_intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoIngestionEvent {
    private UUID videoId;
    private String tenantId;
    private String filePath;
}
