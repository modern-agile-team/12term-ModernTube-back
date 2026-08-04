package com.moderntube.moderntubo_backend.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VideoUploadResponse {
    private Long videoId;
    private String filename;
    private double duration;
    private Integer width;
    private Integer height;
    private String codec;
    private Long bitrate;
}
