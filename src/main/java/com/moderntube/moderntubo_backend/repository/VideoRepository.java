package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByIsHiddenFalseAndIsActiveTrue(Pageable pageable);
}
