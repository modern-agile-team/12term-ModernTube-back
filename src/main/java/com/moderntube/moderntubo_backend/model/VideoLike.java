package com.moderntube.moderntubo_backend.model;

import jakarta.persistence.*;

@Entity(name = "VIDEO_LIKE")
@IdClass(VideoLikeId.class)
public class VideoLike {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIDEO_ID")
    private Video video;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    public VideoLike() {}

    public VideoLike(Video video, User user) {
        this.video = video;
        this.user = user;
    }

    public Video getVideo() {
        return video;
    }

    public User getUser() {
        return user;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
