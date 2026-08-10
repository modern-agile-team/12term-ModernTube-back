package com.moderntube.moderntubo_backend.cache;

import com.moderntube.moderntubo_backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EmailVerificationCache {

    private static final long COOLDOWN_MS = 60_000; // 재전송 대기 시간 (60초)

    private final StringRedisTemplate redisTemplate;
    private final long verificationDurationMs;

    public EmailVerificationCache(StringRedisTemplate redisTemplate,
                                  @Value("${app.token.email.verification.duration}") long verificationDurationMs) {
        this.verificationDurationMs = verificationDurationMs;
        this.redisTemplate = redisTemplate;
    }

    public void saveCode(String email, String code) {
        Long remainingMs = redisTemplate.getExpire(email, TimeUnit.MILLISECONDS);
        if (remainingMs != null && remainingMs > (verificationDurationMs - COOLDOWN_MS)) {
            throw new BadRequestException("잠시 후 다시 시도해주세요.");
        }

        redisTemplate.opsForValue().set(email, code, verificationDurationMs, TimeUnit.MILLISECONDS);
    }

    public boolean verifyCode(String email, String inputCode) {
        String savedCode = redisTemplate.opsForValue().get(email);
        return savedCode != null && savedCode.equals(inputCode);
    }

    public void removeCode(String email) {
        redisTemplate.delete(email);
    }

}
