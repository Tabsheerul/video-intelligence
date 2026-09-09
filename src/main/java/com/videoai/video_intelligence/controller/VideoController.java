package com.videoai.video_intelligence.controller;

import com.videoai.video_intelligence.dto.ApiResponse;
import com.videoai.video_intelligence.dto.VideoIngestionEvent;
import com.videoai.video_intelligence.entity.Video;
import com.videoai.video_intelligence.service.KafkaProducerService;
import com.videoai.video_intelligence.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/videos")
@Tag(name = "Video Ingestion", description = "Endpoints for uploading and ingesting videos into the AI processing pipeline")
public class VideoController {

    private final VideoService videoService;
    private final KafkaProducerService kafkaProducerService;

    public VideoController(VideoService videoService, KafkaProducerService kafkaProducerService) {
        this.videoService = videoService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Operation(
            summary = "Upload a video for AI processing",
            description = "Accepts an MP4 file (max 100MB), saves it locally, publishes an ingestion event "
                    + "to Kafka, and immediately returns 202 ACCEPTED. The video is then asynchronously "
                    + "processed through audio extraction, transcription, and embedding generation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Video accepted for processing"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file type or missing parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Video>> uploadVideo(
            @Parameter(description = "The MP4 video file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Title of the video", required = true)
            @RequestParam("title") String title,
            @Parameter(description = "Optional description of the video")
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "Tenant ID for multi-tenancy", required = true)
            @RequestParam("tenantId") String tenantId) {

        Video savedVideo = videoService.uploadVideo(file, title, description, tenantId);
        
        // Step 3: Publish to Kafka
        VideoIngestionEvent event = new VideoIngestionEvent(savedVideo.getId(), savedVideo.getTenantId(), savedVideo.getFilePath());
        kafkaProducerService.publishVideoIngestionEvent(event);
        
        return new ResponseEntity<>(ApiResponse.success("Video uploaded successfully. Processing started.", savedVideo), HttpStatus.ACCEPTED);
    }
}
