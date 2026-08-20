package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.VideoLike;
import com.moderntube.moderntubo_backend.model.VideoLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLikeId> {
    boolean existsByVideo_VideoIdAndUser_Id(Long videoId, Long userId);
    void deleteByVideo_VideoIdAndUser_Id(Long videoId, Long userId);
    long countByVideo_VideoId(Long videoId);
}
