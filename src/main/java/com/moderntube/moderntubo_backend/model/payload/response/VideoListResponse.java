package com.moderntube.moderntubo_backend.model.payload.response;

import java.time.Instant;

public class VideoListResponse {
    private Long videoId;
    private String title;
    private String thumbnailUrl;
    private String uploaderName;
    private Long viewCount;
    private Instant createAt;
}
