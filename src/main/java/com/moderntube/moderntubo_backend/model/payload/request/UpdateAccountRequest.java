package com.moderntube.moderntubo_backend.model.payload.request;

import com.moderntube.moderntubo_backend.validation.annotation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountRequest {

    @NotBlank(message = "현재 비밀번호를 적어주세요.")
    private String currentPassword;

    @NullOrNotBlank(message = "이메일은 공백일 수 없습니다.")
    private String newEmail;

    @NullOrNotBlank(message = "비밀번호는 공백일 수 없습니다.")
    private String newPassword;

    @NullOrNotBlank(message = "이름은 공백일 수 없습니다.")
    private String newNickName;
}
