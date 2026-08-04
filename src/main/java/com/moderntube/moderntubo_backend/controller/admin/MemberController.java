package com.moderntube.moderntubo_backend.controller.admin;

import com.moderntube.moderntubo_backend.model.payload.request.UserRegisterRequest;
import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import com.moderntube.moderntubo_backend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/member")
@Slf4j
@AllArgsConstructor
public class MemberController {

    private final UserService userService;

    /**
     * 사용자 목록 호출, admin 권한 필요
     * @param searchType (username, email, name) : 미지정시 name으로 검색
     * @param searchKeyword
     * @return
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<?> list(
            @RequestParam(value = "searchType", defaultValue = "") String searchType,
            @RequestParam(value = "searchKeyword", defaultValue = "") String searchKeyword
    ) {
        return ResponseEntity.ok(userService.userSearch(searchType, searchKeyword));
    }

    /**
     * 사용자 조회시 권한 목록 호출
     * @param id
     * @return
     */
    @GetMapping("/roleList")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<?> roleList(@RequestParam(value = "id", defaultValue = "") Long id) {
        return ResponseEntity.ok(userService.roleSearch(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<?> save(@Valid @RequestBody UserRegisterRequest registrationRequest) {
        return ResponseEntity.ok(userService.saveUser(registrationRequest));
    }

    @GetMapping("/check/username")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<?> checkUsernameInUse(@RequestParam("username") String username) {
        boolean usernameExists = userService.usernameAlreadyExists(username);
        return ResponseEntity.ok(new ApiResponse(!usernameExists, usernameExists ? "이미 사용중인 아이디입니다." : "사용 가능한 아이디입니다."));
    }

    @GetMapping("/check/email")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<?> checkEmailInUse(@RequestParam("email") String email) {
        boolean emailExists = userService.emailAlreadyExists(email);
        return ResponseEntity.ok(new ApiResponse(!emailExists, emailExists ? "이미 사용중인 이메일입니다." : "사용 가능한 이메일입니다."));
    }

}
