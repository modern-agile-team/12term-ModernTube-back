/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moderntube.moderntubo_backend.model.payload.request;

import com.moderntube.moderntubo_backend.model.payload.DeviceInfo;
import com.moderntube.moderntubo_backend.validation.annotation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "아이디", example = "modernTube_user", requiredMode = Schema.RequiredMode.REQUIRED)
    @NullOrNotBlank(message = "아이디는 필수 항목입니다.")
    private String username;

    @Schema(description = "비밀번호", example = "Passw0rd!23", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @Schema(description = "로그인 장치 정보 (기기별 refresh token 발급/관리에 사용)", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "장치정보는 필수 항목입니다.")
    private DeviceInfo deviceInfo;

}
