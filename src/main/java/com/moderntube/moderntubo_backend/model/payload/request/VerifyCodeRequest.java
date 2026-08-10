package com.moderntube.moderntubo_backend.model.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class VerifyCodeRequest {

    @Schema(description = "인증할 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @Schema(description = "메일로 받은 인증번호", example = "482913", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "인증번호를 입력해주세요.")
    private String code;
}
