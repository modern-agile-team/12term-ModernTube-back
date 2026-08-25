package com.moderntube.moderntubo_backend.model;

import java.io.Serializable;
import java.util.Objects;

public class VideoLikeId implements Serializable {

    private Long video;
    private Long user;

    public VideoLikeId() {}

    public VideoLikeId(Long video, Long user) {
        this.video = video;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoLikeId that = (VideoLikeId) o;
        return Objects.equals(video, that.video) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(video, user);
    }
}
