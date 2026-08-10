package com.moderntube.moderntubo_backend.controller.auth;

import com.moderntube.moderntubo_backend.annotation.CurrentUser;
import com.moderntube.moderntubo_backend.event.OnUserLogoutSuccessEvent;
import com.moderntube.moderntubo_backend.exception.BadRequestException;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.payload.request.LogOutRequest;
import com.moderntube.moderntubo_backend.model.payload.request.UpdateAccountRequest;
import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import com.moderntube.moderntubo_backend.model.payload.response.UserResponse;
import com.moderntube.moderntubo_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Objects;

@RestController
@RequestMapping("/api/user")
@Slf4j
@AllArgsConstructor
public class UserController {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 로그인 된 사용자 본인의 프로필 정보를 조회한다.
     * @param currentUser JWT에서 추출된 인증된 사용자 정보
     * @return 사용자의 username, email, 권한, id, 활성화 여부, 이름을 담은 UserResponse
     */
    @Operation(
            summary = "내 정보 조회",
            description = "Authorization 헤더에 담긴 Bearer 토큰을 기반으로 현재 로그인한 사용자의 프로필 정보를 반환합니다.")
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserProfile(@CurrentUser CustomUserDetails currentUser) {
        log.info(currentUser.getEmail() + " has role: " + currentUser.getRoles() + " username: " + currentUser.getUsername());
        UserResponse userResponse = new UserResponse(currentUser.getUsername(), currentUser.getEmail(), currentUser.getRoles(), currentUser.getId(), currentUser.getActive(), currentUser.getName());
        return ResponseEntity.ok(userResponse);
    }

    /**
     * 로그인 된 사용자 본인의 이메일, 비밀번호, 닉네임을 변경한다.
     * @param currentUser JWT에서 추출된 인증된 사용자 정보, 변경 대상 유저를 식별하는 데 사용
     * @param updateAccountRequest 현재 비밀번호와 변경할 이메일/비밀번호/닉네임 정보를 담은 요청 객체
     * @return 변경 처리 결과를 담은 ApiResponse
     */
    @Operation(summary = "이메일, 비밀번호, 닉네임 재설정.")
    @PostMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateMyAccount(
            @CurrentUser CustomUserDetails currentUser,
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest
    ) {
        if (!passwordEncoder.matches(updateAccountRequest.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("기존 비밀번호와 맞지 않습니다.");
        } else if (updateAccountRequest.getNewEmail() != null
                && !updateAccountRequest.getNewEmail().equals(currentUser.getEmail())
                && userService.existsByEmail(updateAccountRequest.getNewEmail())
        ) {
            throw new BadRequestException("이미 존재하는 이메일입니다.");
        }

        userService.changeUserInfo(currentUser.getId(), updateAccountRequest);
        return ResponseEntity.ok(new ApiResponse(true, "변경되었습니다."));
    }

    /**
     * 로그아웃
     * @param customUserDetails JWT에서 추출된 인증된 사용자 정보
     * @param logOutRequest 로그아웃할 기기 정보를 담은 요청 객체
     * @return 로그아웃 처리 결과를 담은 ApiResponse
     */
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@CurrentUser CustomUserDetails customUserDetails,
                                     @Valid @RequestBody LogOutRequest logOutRequest) {
        log.info(customUserDetails.toString());
        log.info(logOutRequest.toString());
        userService.logoutUser(customUserDetails, logOutRequest);
        Object credentials = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getCredentials();

        OnUserLogoutSuccessEvent logoutSuccessEvent = new OnUserLogoutSuccessEvent(customUserDetails.getEmail(), Objects.requireNonNull(credentials).toString(), logOutRequest);
        applicationEventPublisher.publishEvent(logoutSuccessEvent);
        return ResponseEntity.ok(new ApiResponse(true, "로그아웃 되었습니다."));
    }

}
