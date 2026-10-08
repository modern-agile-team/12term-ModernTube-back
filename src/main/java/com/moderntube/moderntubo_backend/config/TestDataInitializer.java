package com.moderntube.moderntubo_backend.config;

import com.moderntube.moderntubo_backend.model.payload.request.RegistrationRequest;
import com.moderntube.moderntubo_backend.repository.UserRepository;
import com.moderntube.moderntubo_backend.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 해당 Config는 테스트 유저 생성 설정입니다.

@Component
@Profile("dev")
@AllArgsConstructor
@ConditionalOnProperty(name = "app.test-data.enabled", havingValue = "true", matchIfMissing = true)
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("testuser")) {
            return;
        }
        authService.createTestAccount(new RegistrationRequest("testuser", "test@example.com", "test1234", "테스트유저"));
    }
}
