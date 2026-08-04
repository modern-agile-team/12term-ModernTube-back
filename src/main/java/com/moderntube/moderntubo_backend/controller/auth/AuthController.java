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
    @Operation(summary = "이메일 사용 여부")
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
    @Operation(summary = "아이디 사용여부 확인")
    @GetMapping("/check/username")
    public ResponseEntity<?> checkUsernameInUse(@Parameter(description = "확인할 아이디", required = true) @RequestParam(
            "username") String username) {
        boolean usernameExists = authService.usernameAlreadyExists(username);
        return ResponseEntity.ok(new ApiResponse(usernameExists, usernameExists ? "이미 사용중인 아이디입니다.": ""));
    }

    /**
     * 로그인 성공시 access token, refresh token 반환
     * @param loginRequest 로그인에 필요한 아이디, 비밀번호, 기기 정보를 담은 요청 객체
     * @return 발급된 accessToken과 refreshToken을 담은 JwtAuthenticationResponse
     */
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        log.info("login user >> " + loginRequest.getPassword() + " // " + loginRequest.getUsername());

        Authentication authentication = authService.authenticateUser(loginRequest)
                .orElseThrow(() -> new UserLoginException("Couldn't login user [" + loginRequest + "]"));

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("Logged in User returned [API]: " + customUserDetails.getUsername());

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
    @Operation(summary = "리프레시 토큰")
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
    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request) {
        log.info(request.toString());
        return authService.registerUser(request).map(user -> {
            return ResponseEntity.ok(new ApiResponse(true, "등록되었습니다."));
        }).orElseThrow(() -> new UserRegistrationException(request.getUsername(), "가입오류"));
    }

}
