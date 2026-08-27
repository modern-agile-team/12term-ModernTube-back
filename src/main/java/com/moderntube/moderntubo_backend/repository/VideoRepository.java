package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Query("SELECT v FROM VIDEOS v JOIN FETCH v.uploader WHERE v.isHidden = false AND v.isActive = true")
    Page<Video> findByIsHiddenFalseAndIsActiveTrue(Pageable pageable);
}
