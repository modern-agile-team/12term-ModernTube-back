package com.moderntube.moderntubo_backend.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class VideoListResponse {
    private Long videoId;
    private String title;
    private String thumbnailUrl;
    private String uploaderName;
    private Long viewCount;
    private Instant createdAt;
}
