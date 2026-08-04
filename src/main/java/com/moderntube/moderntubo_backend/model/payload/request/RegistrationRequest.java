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

import com.moderntube.moderntubo_backend.validation.annotation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @Schema(description = "아이디 (영문 대소문자와 숫자만 허용, 6자 이상, 특수문자·공백·한글 불가)",
            example = "modernTube6",
            pattern = "^[A-Za-z0-9]{6,}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NullOrNotBlank(message = "Registration username can be null but not blank")
    private String username;

    @Schema(description = "이메일 (local@domain.tld 형식, 최상위 도메인은 2~3자 영문자만 허용)",
            example = "user@example.com",
            pattern = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\\.[a-zA-Z]{2,3}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NullOrNotBlank(message = "Registration email can be null but not blank")
    private String email;

    @Schema(description = "비밀번호 (8자 이상, 숫자 1개 이상과 영문자 1개 이상 포함)",
            example = "Passw0rd123",
            pattern = "^(?=.*\\d)(?=.*[a-z])(?=.*[a-zA-Z]).{8,}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Registration password cannot be null")
    private String password;

    @Schema(description = "이름 (영문자로 시작, 영문+숫자+'-'+'_' 조합 4~33자, 한글 불가)",
            example = "gildong-hong",
            pattern = "^[a-zA-Z][a-zA-Z0-9_-]{3,32}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
