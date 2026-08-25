package com.moderntube.moderntubo_backend.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 제목, 업로더 정보, 메타데이터, 조회수, 좋아요수, 댓글수, 내가 좋아요 눌렀는지 여부
@Getter
@AllArgsConstructor
public class VideoDetailResponse {
    private String videoTitle;
    private String uploaderName;
    private VideoUploadResponse videoInfo;
    private Long videoView;
    private Long videoLike;
    private Long commentCount;
    private Boolean myLike;
}
