package com.moderntube.moderntubo_backend.model;

import com.moderntube.moderntubo_backend.model.audit.DateAudit;
import jakarta.persistence.*;
import lombok.ToString;

@Entity(name = "Comment")
@ToString
public class Comment extends DateAudit {

    @Id
    @Column(name = "comment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIDEO_ID", nullable = false)
    private Video videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User userId;

    @Column(name = "content")
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Video getVideoId() {
        return videoId;
    }

    public User getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }
}
