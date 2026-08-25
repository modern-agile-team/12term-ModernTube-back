package com.moderntube.moderntubo_backend.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private String content;
    private Long authorId;
    private String authorName;
    private Instant createdAt;
}
