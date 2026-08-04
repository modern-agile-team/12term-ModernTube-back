package com.moderntube.moderntubo_backend.controller.auth;

import com.moderntube.moderntubo_backend.annotation.CurrentUser;
import com.moderntube.moderntubo_backend.event.OnUserLogoutSuccessEvent;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.payload.request.LogOutRequest;
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
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
@Slf4j
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 현재 사용자의 프로필 리턴
     * @param currentUser
     * @return
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
     * 로그아웃
     * @param customUserDetails
     * @param logOutRequest
     * @return
     */
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@CurrentUser CustomUserDetails customUserDetails,
                                     @Valid @RequestBody LogOutRequest logOutRequest) {
        log.info(customUserDetails.toString());
        log.info(logOutRequest.toString());
        userService.logoutUser(customUserDetails, logOutRequest);
        Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();

        OnUserLogoutSuccessEvent logoutSuccessEvent = new OnUserLogoutSuccessEvent(customUserDetails.getEmail(), credentials.toString(), logOutRequest);
        applicationEventPublisher.publishEvent(logoutSuccessEvent);
        return ResponseEntity.ok(new ApiResponse(true, "로그아웃 되었습니다."));
    }

}
