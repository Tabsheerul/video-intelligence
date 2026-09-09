package com.videoai.video_intelligence.service;

import com.videoai.video_intelligence.config.KafkaConfig;
import com.videoai.video_intelligence.dto.VideoIngestionEvent;
import com.videoai.video_intelligence.entity.Video;
import com.videoai.video_intelligence.entity.VideoChunk;
import com.videoai.video_intelligence.entity.VideoStatus;
import com.videoai.video_intelligence.repository.VideoChunkRepository;
import com.videoai.video_intelligence.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service
public class KafkaConsumerService {

    private final VideoRepository videoRepository;
    private final VideoChunkRepository videoChunkRepository;
    private final AudioExtractionService audioExtractionService;
    private final TranscriptionService transcriptionService;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaConsumerService(VideoRepository videoRepository,
                                VideoChunkRepository videoChunkRepository,
                                AudioExtractionService audioExtractionService,
                                TranscriptionService transcriptionService,
                                EmbeddingGenerationService embeddingGenerationService) {
        this.videoRepository = videoRepository;
        this.videoChunkRepository = videoChunkRepository;
        this.audioExtractionService = audioExtractionService;
        this.transcriptionService = transcriptionService;
        this.embeddingGenerationService = embeddingGenerationService;
    }

    @KafkaListener(topics = KafkaConfig.VIDEO_INGESTION_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeVideoIngestionEvent(String eventJson) {
        File wavFile = null;
        try {
            VideoIngestionEvent event = objectMapper.readValue(eventJson, VideoIngestionEvent.class);
            log.info("========== AI PROCESSING PIPELINE START ==========");
            log.info("Received Video Ingestion Event for Video ID: {}", event.getVideoId());

            // Step 1: Find the video and update status to PROCESSING
            Video video = videoRepository.findById(event.getVideoId()).orElse(null);
            if (video == null) {
                log.error("Video with ID {} not found in database. Aborting processing.", event.getVideoId());
                return;
            }

            video.setStatus(VideoStatus.PROCESSING);
            videoRepository.save(video);
            log.info("[Step 1/5] Status updated to PROCESSING");

            // Step 2: Extract audio from the video file using FFmpeg (JAVE2)
            log.info("[Step 2/5] Extracting audio from video: {}", event.getFilePath());
            wavFile = audioExtractionService.extractAudio(event.getFilePath());
            log.info("[Step 2/5] Audio extraction complete: {}", wavFile.getAbsolutePath());

            // Step 3: Send audio to Google Cloud Speech-to-Text for transcription
            log.info("[Step 3/5] Sending audio to Google Cloud Speech-to-Text...");
            List<TranscriptionService.TranscriptChunk> transcriptChunks = transcriptionService.transcribe(wavFile);
            log.info("[Step 3/5] Transcription complete. Got {} chunks", transcriptChunks.size());

            if (transcriptChunks.isEmpty()) {
                log.warn("No speech detected in video. Marking as COMPLETED with zero chunks.");
                video.setStatus(VideoStatus.COMPLETED);
                videoRepository.save(video);
                return;
            }

            // Step 4: Generate embeddings for each transcript chunk
            log.info("[Step 4/5] Generating embeddings for {} transcript chunks...", transcriptChunks.size());
            for (int i = 0; i < transcriptChunks.size(); i++) {
                TranscriptionService.TranscriptChunk chunk = transcriptChunks.get(i);

                log.info("  Chunk {}/{}: [{}s - {}s] \"{}\"",
                        i + 1, transcriptChunks.size(),
                        String.format("%.1f", chunk.getStartTimestamp()),
                        String.format("%.1f", chunk.getEndTimestamp()),
                        chunk.getTranscript().substring(0, Math.min(chunk.getTranscript().length(), 60)));

                float[] embedding = embeddingGenerationService.generateEmbedding(chunk.getTranscript());

                VideoChunk videoChunk = new VideoChunk();
                videoChunk.setVideo(video);
                videoChunk.setStartTimestamp(chunk.getStartTimestamp());
                videoChunk.setEndTimestamp(chunk.getEndTimestamp());
                videoChunk.setTranscript(chunk.getTranscript());
                videoChunk.setEmbedding(embedding);

                videoChunkRepository.save(videoChunk);
            }
            log.info("[Step 4/5] All embeddings generated and saved to database");

            // Step 5: Update status to COMPLETED
            video.setStatus(VideoStatus.COMPLETED);
            videoRepository.save(video);
            log.info("[Step 5/5] Status updated to COMPLETED");
            log.info("========== AI PROCESSING PIPELINE COMPLETE ==========");

        } catch (Exception e) {
            log.error("Error processing video ingestion event", e);
            throw new RuntimeException(e);
        } finally {
            // Clean up temporary WAV file
            if (wavFile != null && wavFile.exists()) {
                boolean deleted = wavFile.delete();
                log.info("Temporary WAV file cleanup: {}", deleted ? "deleted" : "failed to delete");
            }
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
