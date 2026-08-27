package com.moderntube.moderntubo_backend.controller.auth;

import com.moderntube.moderntubo_backend.exception.*;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.payload.request.*;
import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import com.moderntube.moderntubo_backend.model.payload.response.JwtAuthenticationResponse;
import com.moderntube.moderntubo_backend.model.token.RefreshToken;
import com.moderntube.moderntubo_backend.security.JwtTokenProvider;
import com.moderntube.moderntubo_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    /**
     * 이메일 사용여부 확인 메서드
     * @param email 중복 확인할 이메일 주소
     * @return 이메일 사용 여부와 안내 메시지를 담은 ApiResponse
     */
    @Operation(
            summary = "이메일 사용 여부",
            description = "회원가입 폼에서 이메일을 입력한 직후, 회원가입 API를 호출하기 전에 먼저 호출해서 "
                    + "중복 여부를 미리 확인하는 용도입니다."
    )
    @GetMapping("/check/email")
    public ResponseEntity<?> checkEmailInUse(@Parameter(description = "확인할 이메일", required = true) @RequestParam("email") String email) {
        boolean emailExists = authService.emailAlreadyExists(email);
        return ResponseEntity.ok(new ApiResponse(emailExists, emailExists ? "이미 사용중인 이메일입니다." : ""));
    }

    /**
     * username 사용여부 확인
     * @param username 중복 확인할 아이디
     * @return 아이디 사용 여부와 안내 메시지를 담은 ApiResponse
     */
    @Operation(
            summary = "아이디 사용여부 확인",
            description = "회원가입 폼에서 아이디를 입력한 직후, 회원가입 API를 호출하기 전에 먼저 호출해서 "
                    + "중복 여부를 미리 확인하는 용도입니다."
    )
    @GetMapping("/check/username")
    public ResponseEntity<?> checkUsernameInUse(@Parameter(description = "확인할 아이디", required = true) @RequestParam(
            "username") String username) {
        boolean usernameExists = authService.usernameAlreadyExists(username);
        return ResponseEntity.ok(new ApiResponse(usernameExists, usernameExists ? "이미 사용중인 아이디입니다.": ""));
    }

    /**
     * 로그인 성공시 access token, refresh token 반환
     * @param loginRequest 로그인에 필요한 아이디, 비밀번호, 기기 정보를 담은 요청 객체
     * @return
     */
    @Operation(
            summary = "로그인",
            description = "아이디/비밀번호와 함께 기기 정보(deviceInfo)를 반드시 같이 보내야 합니다 (기기별로 로그인 세션을 "
                    + "독립적으로 관리하기 때문). 성공하면 accessToken과 refreshToken을 발급합니다. "
                    + "accessToken은 이후 요청의 Authorization 헤더에 'Bearer {token}' 형식으로 담아 사용하고, "
                    + "refreshToken은 accessToken이 만료됐을 때 재발급받는 용도로 별도 보관해야 합니다. "
                    + "이메일 인증을 완료하지 않은 계정은 로그인이 거부됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        log.info("login user >> " + loginRequest.getPassword() + " // " + loginRequest.getUsername());

        Authentication authentication = authService.authenticateUser(loginRequest)
                .orElseThrow(() -> new UserLoginException("Couldn't login user [" + loginRequest + "]"));

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("Logged in User returned [API]: " + Objects.requireNonNull(customUserDetails).getUsername());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authService.createAndPersistRefreshTokenForDevice(authentication, loginRequest)
                .map(RefreshToken::getToken)
                .map(refreshToken -> {
                    String jwtToken = authService.generateToken(customUserDetails);
                    return ResponseEntity.ok(new JwtAuthenticationResponse(jwtToken, refreshToken, tokenProvider.getExpiryDuration()));
                })
                .orElseThrow(() -> new UserLoginException("Couldn't create refresh token for: [" + loginRequest + "]"));
    }

    /**
     * 특정 장치에 대한 refresh token 을 사용하여 만료된 jwt token 을 갱신 후 새로운 token 을 반환
     * @param tokenRefreshRequest 토큰 갱신에 사용할 refresh token을 담은 요청 객체
     * @return 재발급된 accessToken과 기존 refreshToken을 담은 JwtAuthenticationResponse
     */
    @Operation(
            summary = "리프레시 토큰",
            description = "accessToken이 만료되었을 때, 로그인 시 발급받은 refreshToken으로 새 accessToken을 재발급받습니다. "
                    + "refreshToken은 로그인했던 기기마다 다르게 발급되므로, 반드시 해당 기기가 로그인할 때 받았던 "
                    + "refreshToken을 그대로 사용해야 합니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshJwtToken(@Valid @RequestBody TokenRefreshRequest tokenRefreshRequest) {

        log.info(tokenRefreshRequest.toString());

        return authService.refreshJwtToken(tokenRefreshRequest)
                .map(updatedToken -> {
                    String refreshToken = tokenRefreshRequest.getRefreshToken();
                    log.info("Created new Jwt Auth token: " + updatedToken);
                    return ResponseEntity.ok(new JwtAuthenticationResponse(updatedToken, refreshToken, tokenProvider.getExpiryDuration()));
                })
                .orElseThrow(() -> new TokenRefreshException(tokenRefreshRequest.getRefreshToken(), "토큰 갱신 중 오류가 발생했습니다. 다시 로그인 해 주세요."));
    }

    /**
     * 회원 가입
     * @param request 회원가입에 필요한 아이디, 이메일, 비밀번호, 이름을 담은 요청 객체
     * @return 가입 처리 결과를 담은 ApiResponse
     */
    @Operation(
            summary = "회원가입",
            description = "가입 완료 직후에는 이메일 인증이 안 된 상태라 바로 로그인할 수 없습니다. "
                    + "가입 후 '/api/auth/send-code'로 인증번호를 받아 '/api/auth/verify-code'로 인증을 "
                    + "완료해야 로그인이 가능합니다."
    )
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request) {
        log.info(request.toString());
        return authService.registerUser(request).map(user -> {
            return ResponseEntity.ok(new ApiResponse(true, "등록되었습니다."));
        }).orElseThrow(() -> new UserRegistrationException(request.getUsername(), "가입오류"));
    }

    /**
     * 이메일로 인증번호 발송
     * @param email 인증번호를 받을 이메일
     * @return 발송 처리 결과를 담은 ApiResponse
     */
    @Operation(
            summary = "메일 인증번호를 전송",
            description = "회원가입 직후, 로그인 전에 호출합니다. 같은 이메일로는 60초 이내에 재전송을 요청할 수 없으니, "
                    + "재전송 버튼은 그 시간만큼 비활성화하는 걸 권장합니다."
    )
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCodeToUser(
            @Parameter(description = "인증번호를 받을 이메일", required = true)
            @RequestParam("email") String email
    ) {
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(new ApiResponse(true, "전송되었습니다."));
    }

    /**
     * 이메일 인증번호 검증
     * @param verifyCodeRequest 이메일과 인증번호를 담은 요청 객체
     * @return 검증 처리 결과를 담은 ApiResponse
     */
    @Operation(
            summary = "메일 인증번호 검증",
            description = "인증에 성공하면 계정의 이메일 인증 상태가 완료로 바뀌며, 그 이후부터 로그인이 가능해집니다."
    )
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyCodeRequest) {
        authService.verifyEmailCode(verifyCodeRequest.getEmail(), verifyCodeRequest.getCode());
        return ResponseEntity.ok(new ApiResponse(true, "인증이 완료되었습니다."));
    }

}
