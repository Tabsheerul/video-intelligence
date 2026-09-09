package com.videoai.video_intelligence.service;

import com.google.cloud.speech.v1.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageException;

/**
 * Sends extracted WAV audio to Google Cloud Speech-to-Text API
 * and returns a list of transcript chunks with timestamps.
 *
 * Each chunk represents a contiguous segment of spoken words,
 * grouped together by time proximity (within CHUNK_GAP_THRESHOLD_SECONDS).
 */
@Slf4j
@Service
public class TranscriptionService {

    /**
     * Maximum gap (in seconds) between consecutive words before starting a new chunk.
     * If two words are separated by more than this gap, they belong to different chunks.
     */
    private static final double CHUNK_GAP_THRESHOLD_SECONDS = 5.0;

    /**
     * A single chunk of transcribed text with start and end timestamps.
     */
    @Data
    @AllArgsConstructor
    public static class TranscriptChunk {
        private double startTimestamp;
        private double endTimestamp;
        private String transcript;
    }

    /**
     * Transcribes a WAV audio file using Google Cloud Speech-to-Text.
     *
     * Uses the LongRunningRecognize API (asynchronous) which supports files
     * of any length. The response includes word-level timestamps which are
     * grouped into logical chunks.
     *
     * @param audioFile the WAV file to transcribe (must be 16kHz mono LINEAR16)
     * @return a list of TranscriptChunk objects with timestamps and text
     * @throws Exception if the API call or file reading fails
     */
    public List<TranscriptChunk> transcribe(File audioFile) throws Exception {
        log.info("Starting transcription for audio file: {} (size: {} bytes)",
                audioFile.getName(), audioFile.length());

        Storage storage = StorageOptions.getDefaultInstance().getService();
        String projectId = storage.getOptions().getProjectId();
        String bucketName = "video-int-tmp-" + (projectId != null ? projectId : java.util.UUID.randomUUID().toString().substring(0, 8)).toLowerCase();

        // Create bucket if it doesn't exist
        try {
            if (storage.get(bucketName) == null) {
                log.info("Creating temporary Cloud Storage bucket: {}", bucketName);
                storage.create(BucketInfo.of(bucketName));
            }
        } catch (StorageException e) {
            // Bucket might already exist and be owned by someone else, or permission denied. We just warn and proceed.
            log.warn("Could not verify or create bucket '{}', will attempt to use it anyway. Error: {}", bucketName, e.getMessage());
        }

        String objectName = java.util.UUID.randomUUID().toString() + "_" + audioFile.getName();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        log.info("Uploading audio to Google Cloud Storage: gs://{}/{}", bucketName, objectName);
        storage.create(blobInfo, Files.readAllBytes(audioFile.toPath()));

        String gcsUri = "gs://" + bucketName + "/" + objectName;

        // Configure recognition settings
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.OGG_OPUS)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .setEnableWordTimeOffsets(true)  // Critical: enables word-level timestamps
                .setModel("latest_long")          // Best model for long-form content
                .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setUri(gcsUri)
                .build();

        List<TranscriptChunk> chunks = new ArrayList<>();

        // Use try-with-resources to auto-close the SpeechClient
        try (SpeechClient speechClient = SpeechClient.create()) {
            log.info("Sending audio to Google Cloud Speech-to-Text API from GCS...");

            // Use longRunningRecognize for files that may be longer than 1 minute
            var operationFuture = speechClient.longRunningRecognizeAsync(config, audio);
            LongRunningRecognizeResponse response = operationFuture.get();

            log.info("Received {} result(s) from Speech-to-Text API", response.getResultsCount());

            // Collect all words with their timestamps across all results
            List<WordInfo> allWords = new ArrayList<>();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                if (result.getAlternativesCount() > 0) {
                    SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    allWords.addAll(alternative.getWordsList());
                }
            }

            if (allWords.isEmpty()) {
                log.warn("No words found in transcription. Audio may be silent or corrupted.");
                return chunks;
            }

            log.info("Total words transcribed: {}", allWords.size());

            // Group words into logical chunks based on time gaps
            chunks = groupWordsIntoChunks(allWords);

            log.info("Created {} transcript chunks from {} words", chunks.size(), allWords.size());
        } finally {
            log.info("Cleaning up temporary file from Google Cloud Storage: {}", gcsUri);
            try {
                storage.delete(blobId);
            } catch (Exception e) {
                log.error("Failed to delete temporary file from GCS: {}", e.getMessage());
            }
        }

        return chunks;
    }

    /**
     * Groups a flat list of words into logical chunks.
     * A new chunk is started when the gap between consecutive words
     * exceeds CHUNK_GAP_THRESHOLD_SECONDS.
     */
    private List<TranscriptChunk> groupWordsIntoChunks(List<WordInfo> words) {
        List<TranscriptChunk> chunks = new ArrayList<>();

        double chunkStart = getTimestampSeconds(words.get(0).getStartTime());
        double chunkEnd = getTimestampSeconds(words.get(0).getEndTime());
        StringBuilder chunkText = new StringBuilder(words.get(0).getWord());

        for (int i = 1; i < words.size(); i++) {
            WordInfo word = words.get(i);
            double wordStart = getTimestampSeconds(word.getStartTime());
            double wordEnd = getTimestampSeconds(word.getEndTime());

            // Check if this word is too far from the previous one — start a new chunk
            if (wordStart - chunkEnd > CHUNK_GAP_THRESHOLD_SECONDS) {
                chunks.add(new TranscriptChunk(chunkStart, chunkEnd, chunkText.toString().trim()));

                // Reset for new chunk
                chunkStart = wordStart;
                chunkText = new StringBuilder();
            }

            chunkText.append(" ").append(word.getWord());
            chunkEnd = wordEnd;
        }

        // Don't forget the last chunk
        if (chunkText.length() > 0) {
            chunks.add(new TranscriptChunk(chunkStart, chunkEnd, chunkText.toString().trim()));
        }

        return chunks;
    }

    /**
     * Converts a protobuf Duration to a double representing seconds.
     */
    private double getTimestampSeconds(com.google.protobuf.Duration duration) {
        return duration.getSeconds() + duration.getNanos() / 1_000_000_000.0;
    }
}
