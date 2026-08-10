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
package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.cache.EmailVerificationCache;
import com.moderntube.moderntubo_backend.cache.LoginAttemptCache;
import com.moderntube.moderntubo_backend.exception.BadRequestException;
import com.moderntube.moderntubo_backend.exception.ResourceAlreadyInUseException;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.User;
import com.moderntube.moderntubo_backend.model.UserDevice;
import com.moderntube.moderntubo_backend.model.payload.request.LoginRequest;
import com.moderntube.moderntubo_backend.model.payload.request.RegistrationRequest;
import com.moderntube.moderntubo_backend.model.payload.request.TokenRefreshRequest;
import com.moderntube.moderntubo_backend.model.token.RefreshToken;
import com.moderntube.moderntubo_backend.security.JwtTokenProvider;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDeviceService userDeviceService;
    private final MailService mailService;
    private final EmailVerificationCache emailVerificationCache;
    private final LoginAttemptCache loginAttemptCache;

    /**
     * 이메일 인증번호 발송
     * @param email 인증번호를 받을 이메일
     */
    public void sendVerificationCode(String email) {
        try {
            mailService.sendVerificationEmail(email);
        } catch (MessagingException | IOException e) {
            log.error("이메일 발송 실패: {}", email, e);
            throw new BadRequestException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 이메일 인증번호 검증 후 이메일 인증 완료 처리
     * @param email 인증할 이메일
     * @param code 사용자가 입력한 인증번호
     */
    public void verifyEmailCode(String email, String code) {
        if (!emailVerificationCache.verifyCode(email, code)) {
            throw new BadRequestException("인증번호가 일치하지 않거나 만료되었습니다.");
        }
        userService.markEmailVerified(email);
        emailVerificationCache.removeCode(email);
    }

    /**
     * 사용자 등록
     * 등록되면 user object 생성
     * @param newRegistrationRequest
     * @return
     */
    public Optional<User> registerUser(RegistrationRequest newRegistrationRequest) {
        String newRegistrationRequestEmail = newRegistrationRequest.getEmail();
        String newRegistrationRequestUserName = newRegistrationRequest.getUsername();
        String userNameReg = "[A-Za-z0-9]{6,}$";
        String emailReg = "[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\\.[a-zA-Z]{2,}$";
        String pwdReg = "(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        String nameReg = "[a-zA-Z][a-zA-Z0-9-_]{3,32}$";

        if (emailAlreadyExists(newRegistrationRequestEmail)) {
            log.error("Email already exists: " + newRegistrationRequestEmail);
            throw new ResourceAlreadyInUseException("Email", "이메일 주소", newRegistrationRequestEmail);
        } else if (usernameAlreadyExists(newRegistrationRequestUserName)) {
            log.error("UserName already exists: " + newRegistrationRequestUserName);
            throw new ResourceAlreadyInUseException("UserName", "아이디 주소", newRegistrationRequestUserName);
        } else if (
                Objects.isNull(newRegistrationRequestUserName) || !Pattern.matches(userNameReg, newRegistrationRequestUserName) ||
                        Objects.isNull(newRegistrationRequestEmail) || !Pattern.matches(emailReg, newRegistrationRequestEmail) ||
                        !Pattern.matches(pwdReg, newRegistrationRequest.getPassword()) ||
                        Objects.isNull(newRegistrationRequest.getName()) || !Pattern.matches(nameReg, newRegistrationRequest.getName())
        ) {
            throw new ResourceAlreadyInUseException("로그인", "정규화", "입력값이 정규식에 맞지 않습니다.");
        }

        log.info("Trying to register new user [" + newRegistrationRequestEmail + "]");
        log.info(newRegistrationRequest.toString());
        User newUser = userService.createUser(newRegistrationRequest);
        User registrationRequest = userService.save(newUser);
        return Optional.ofNullable(registrationRequest);
    }

    /**
     * 회원 가입시 이메일 중복인지 검사
     * 이메일이 이미 있으면 true 아니면 false
     * @param email
     * @return
     */
    public Boolean emailAlreadyExists(String email) {
        return userService.existsByEmail(email);
    }

    /**
     * 회원 가입시 username 중복인지 검사
     * username이 이미 있으면 true 아니면 false
     * @param username
     * @return
     */
    public Boolean usernameAlreadyExists(String username) {
        return userService.existsByUsername(username);
    }

    /**
     * 로그인 수행
     * @param loginRequest 로그인 정보를 받기
     * @return
     */
    public Optional<Authentication> authenticateUser(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        loginAttemptCache.checkBlocked(username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
            loginAttemptCache.resetFailCount(username);
            return Optional.of(authentication);
        } catch (AuthenticationException e) {
            loginAttemptCache.increaseFailCount(username);
            throw e;
        }
    }



    /**
     * 비번 변경시 현재 입력한 비밀번호가 맞는지 확인한다
     * @param currentUser
     * @param password
     * @return
     */
    private Boolean currentPasswordMatches(User currentUser, String password) {
        return passwordEncoder.matches(password, currentUser.getPassword());
    }

    /**
     * token 발행
     * @param customUserDetails
     * @return
     */
    public String generateToken(CustomUserDetails customUserDetails) {
        return tokenProvider.generateToken(customUserDetails);
    }

    /**
     * token 발행 by userId
     */
    private String generateTokenFromUserId(Long userId) {
        return tokenProvider.generateTokenFromUserId(userId);
    }

    /**
     * 사용자 장치에 대한 refresh token 을 만들고 유지
     * 장치가 이미 존재하면 상관 없음
     * 만료 된 토큰이있는 사용하지 않는 장치는 크론 작업으로 정리해야함.
     * 생성 된 토큰은 jwt 내에 캡슐화됨
     * 이전 토큰은 유효하지 않아야하므로 기존 refresh token 을 제거해야함.
     * @param authentication
     * @param loginRequest
     * @return
     */
    public Optional<RefreshToken> createAndPersistRefreshTokenForDevice(Authentication authentication, LoginRequest loginRequest) {
        User currentUser = (User) authentication.getPrincipal();

        userDeviceService.findByUserIDAndDeviceId(currentUser.getId(), loginRequest.getDeviceInfo().getDeviceId())
                .map(UserDevice::getRefreshToken)
                .map(RefreshToken::getId)
                .ifPresent(refreshTokenService::deleteById);

        UserDevice userDevice = userDeviceService.createUserDevice(loginRequest.getDeviceInfo());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken();
        userDevice.setUser(currentUser);
        userDevice.setRefreshToken(refreshToken);
        refreshToken.setUserDevice(userDevice);
        refreshToken = refreshTokenService.save(refreshToken);
        return Optional.ofNullable(refreshToken);
    }

    /**
     * refresh token 을 사용하여 access token 반환
     * @param tokenRefreshRequest
     * @return
     */
    public Optional<String> refreshJwtToken(TokenRefreshRequest tokenRefreshRequest) {
        String requestRefreshToken = tokenRefreshRequest.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshToken -> {
                    refreshTokenService.verifyExpiration(refreshToken);
                    userDeviceService.verifyRefreshAvailability(refreshToken);
                    refreshTokenService.increaseCount(refreshToken);
                    return refreshToken;
                })
                .map(RefreshToken::getUserDevice)
                .map(UserDevice::getUser)
                .map(User::getId)
                .map(this::generateTokenFromUserId);
    }

}
