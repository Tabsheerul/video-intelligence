package com.videoai.video_intelligence.service;

import com.videoai.video_intelligence.config.KafkaConfig;
import com.videoai.video_intelligence.dto.VideoIngestionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishVideoIngestionEvent(VideoIngestionEvent event) {
        log.info("Publishing Video Ingestion Event for Video ID: {}", event.getVideoId());
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.VIDEO_INGESTION_TOPIC, event.getVideoId().toString(), jsonPayload);
        } catch (Exception e) {
            log.error("Failed to serialize and publish video ingestion event", e);
            throw new RuntimeException("Kafka Publish Error", e);
        }
    }
}
