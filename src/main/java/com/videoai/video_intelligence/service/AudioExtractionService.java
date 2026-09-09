package com.videoai.video_intelligence.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;

/**
 * Extracts audio from an MP4 video file and converts it to a WAV file
 * suitable for Google Cloud Speech-to-Text processing.
 *
 * Uses the JAVE2 library which bundles FFmpeg binaries internally,
 * so no external FFmpeg installation is required.
 */
@Slf4j
@Service
public class AudioExtractionService {

    /**
     * Extracts audio from a video file and saves it as a 16kHz mono WAV file.
     *
     * Google Cloud Speech-to-Text works best with:
     * - Format: LINEAR16 (WAV)
     * - Sample rate: 16000 Hz
     * - Channels: 1 (mono)
     *
     * @param videoFilePath absolute path to the source MP4 video file
     * @return the File object pointing to the extracted WAV audio file
     * @throws Exception if audio extraction fails
     */
    public File extractAudio(String videoFilePath) throws Exception {
        File source = new File(videoFilePath);
        if (!source.exists()) {
            throw new IllegalArgumentException("Video file not found: " + videoFilePath);
        }

        // Create the output OGG file path alongside the source video
        String audioFilePath = videoFilePath.replaceAll("\\.[^.]+$", ".ogg");
        File target = new File(audioFilePath);

        log.info("Starting audio extraction: {} -> {}", source.getName(), target.getName());

        // Configure the audio encoding attributes for Speech-to-Text compatibility
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libopus");       // OPUS compression (extreme compression for voice)
        audio.setBitRate(16000);         // 16 kbps bitrate (keeps 1 hour of audio under 7MB)
        audio.setSamplingRate(16000);        // 16kHz sampling rate (Google STT optimal)
        audio.setChannels(1);               // Mono channel

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("ogg");        // Output format: OGG
        attrs.setAudioAttributes(audio);

        // Run the FFmpeg encoding via JAVE2
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, attrs);

        log.info("Audio extraction complete. Output file: {} (size: {} bytes)",
                target.getName(), target.length());

        return target;
    }
}
