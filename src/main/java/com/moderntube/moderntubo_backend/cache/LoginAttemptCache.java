package com.moderntube.moderntubo_backend.cache;

import com.moderntube.moderntubo_backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LoginAttemptCache {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 5 * 60 * 1000; // 5분

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkBlocked(String username) {
        String countStr = redisTemplate.opsForValue().get(key(username));
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= MAX_ATTEMPTS) {
            throw new BadRequestException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public void increaseFailCount(String username) {
        Long count = redisTemplate.opsForValue().increment(key(username));
        if (count != null && count == 1L) {
            redisTemplate.expire(key(username), BLOCK_DURATION_MS, TimeUnit.MILLISECONDS);
        }
        log.info("로그인 실패 [{}] 누적 {}회", username, count);
    }

    public void resetFailCount(String username) {
        redisTemplate.delete(key(username));
    }

    private String key(String username) {
        return "login-attempt:" + username;
    }
}
