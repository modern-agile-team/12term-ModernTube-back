package com.moderntube.moderntubo_backend.repository;

import com.moderntube.moderntubo_backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByVideo_VideoId(Long videoId, Pageable pageable);
    long countByVideo_VideoId(Long videoId);
}
