package com.moderntube.moderntubo_backend.model.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {

    @Schema(description = "댓글 내용", example = "좋은 영상이네요!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;
}
