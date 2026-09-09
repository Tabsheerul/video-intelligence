package com.videoai.video_intelligence.repository;

import com.videoai.video_intelligence.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByTenantId(String tenantId);
}
