package com.videoai.video_intelligence.service;

import com.videoai.video_intelligence.entity.Video;
import com.videoai.video_intelligence.entity.VideoStatus;
import com.videoai.video_intelligence.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final Path fileStorageLocation;

    public VideoService(VideoRepository videoRepository, @Value("${video.storage.path}") String storagePath) {
        this.videoRepository = videoRepository;
        this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Video uploadVideo(MultipartFile file, String title, String description, String tenantId) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("video/mp4")) {
            throw new IllegalArgumentException("Only MP4 video files are supported");
        }

        // Generate a unique filename to prevent overwriting
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create Video entity and save to DB
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setTenantId(tenantId);
            video.setFilePath(targetLocation.toString());
            video.setStatus(VideoStatus.UPLOADED);
            
            return videoRepository.save(video);

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}
