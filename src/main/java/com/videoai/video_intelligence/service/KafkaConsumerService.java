package com.videoai.video_intelligence.service;

import com.videoai.video_intelligence.config.KafkaConfig;
import com.videoai.video_intelligence.dto.VideoIngestionEvent;
import com.videoai.video_intelligence.entity.Video;
import com.videoai.video_intelligence.entity.VideoStatus;
import com.videoai.video_intelligence.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    private final VideoRepository videoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaConsumerService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @KafkaListener(topics = KafkaConfig.VIDEO_INGESTION_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeVideoIngestionEvent(String eventJson) {
        try {
            VideoIngestionEvent event = objectMapper.readValue(eventJson, VideoIngestionEvent.class);
            log.info("Received Video Ingestion Event for Video ID: {}", event.getVideoId());
            
            // Step 1: Update status to PROCESSING
            Video video = videoRepository.findById(event.getVideoId()).orElse(null);
        if (video == null) {
            log.error("Video with ID {} not found in database. Aborting processing.", event.getVideoId());
            return;
        }

        video.setStatus(VideoStatus.PROCESSING);
        videoRepository.save(video);
        
        log.info("Status updated to PROCESSING. Pipeline triggered for file: {}", event.getFilePath());

        // TODO: In Phase 3, we will call Google Cloud Speech-to-Text and Vertex AI here
        
        // Simulate a failure to see DLQ in action (remove in production)
        // throw new RuntimeException("Simulating an AI Processing Failure!");
        } catch (Exception e) {
            log.error("Error processing video ingestion event", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = KafkaConfig.VIDEO_INGESTION_DLQ_TOPIC, groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeDeadLetterEvent(String eventJson) {
        try {
            VideoIngestionEvent event = objectMapper.readValue(eventJson, VideoIngestionEvent.class);
            log.error("Received message on DLQ. Processing totally failed for Video ID: {}", event.getVideoId());

            Video video = videoRepository.findById(event.getVideoId()).orElse(null);
        if (video != null) {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            log.info("Status securely updated to FAILED for Video ID: {}", video.getId());
        }
        } catch (Exception e) {
            log.error("Error processing DLQ event", e);
        }
    }
}
