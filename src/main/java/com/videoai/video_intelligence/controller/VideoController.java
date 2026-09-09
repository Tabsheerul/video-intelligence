package com.videoai.video_intelligence.controller;

import com.videoai.video_intelligence.dto.ApiResponse;
import com.videoai.video_intelligence.dto.VideoIngestionEvent;
import com.videoai.video_intelligence.entity.Video;
import com.videoai.video_intelligence.service.KafkaProducerService;
import com.videoai.video_intelligence.service.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoService videoService;
    private final KafkaProducerService kafkaProducerService;

    public VideoController(VideoService videoService, KafkaProducerService kafkaProducerService) {
        this.videoService = videoService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Video>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("tenantId") String tenantId) {

        Video savedVideo = videoService.uploadVideo(file, title, description, tenantId);
        
        // Step 3: Publish to Kafka
        VideoIngestionEvent event = new VideoIngestionEvent(savedVideo.getId(), savedVideo.getTenantId(), savedVideo.getFilePath());
        kafkaProducerService.publishVideoIngestionEvent(event);
        
        return new ResponseEntity<>(ApiResponse.success("Video uploaded successfully. Processing started.", savedVideo), HttpStatus.ACCEPTED);
    }
}
