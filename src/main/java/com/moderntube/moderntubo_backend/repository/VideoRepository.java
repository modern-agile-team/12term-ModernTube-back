package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
