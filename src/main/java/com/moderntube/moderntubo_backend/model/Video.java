package com.moderntube.moderntubo_backend.model;

import com.moderntube.moderntubo_backend.model.audit.DateAudit;
import com.moderntube.moderntubo_backend.validation.annotation.NullOrNotBlank;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.ToString;

@Entity(name = "VIDEOS")
@ToString
public class Video extends DateAudit {

    @Id
    @Column(name = "VIDEO_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;

    @Column(name = "VIDEO_NAME", nullable = false)
    @NullOrNotBlank(message = "동영상을 넣어주세요.")
    private String videoName;

    @Column(name = "ORIGINAL_FILENAME", nullable = false)
    @NullOrNotBlank(message = "원본 이름을 넣어주세요.")
    private String originalFilename;

    @Column(name = "FILE_PATH", nullable = false)
    @NullOrNotBlank(message = "파일 경로가 필요합니다.")
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User uploader;

    @Column(name = "VIDEO_SIZE", nullable = false)
    @NotNull(message = "비디오 사이즈는 필수 입력입니다.")
    private Long videoSize;

    @Column(name = "VIDEO_WIDTH")
    private Integer videoWidth;

    @Column(name = "VIDEO_HEIGHT")
    private Integer videoHeight;

    @Column(name = "DURATION")
    private double duration;

    @Column(name = "BITRATE")
    private Long bitrate;

    @Column(name = "CODEC")
    private String codec;

    @Column(name = "IS_HIDDEN", nullable = false)
    private boolean isHidden;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive;

    @Column(name = "VIEW_COUNT", nullable = false)
    private Long viewCount = 0L;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }

    public Long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(Long videoSize) {
        this.videoSize = videoSize;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(Integer videoWidth) {
        this.videoWidth = videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(Integer videoHeight) {
        this.videoHeight = videoHeight;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public Long getBitrate() {
        return bitrate;
    }

    public void setBitrate(Long bitrate) {
        this.bitrate = bitrate;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}
