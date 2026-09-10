package com.moderntube.moderntubo_backend.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ViewCountCache {

    private static final long VIEW_TIME = 3 * 60 * 1000; // 3분

    private final StringRedisTemplate redisTemplate;

    public ViewCountCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isNewView(Long userId, Long videoId) {
        return trySet(loginUserKey(userId, videoId));
    }

    public boolean isNewView(String clientIp, Long videoId) {
        return trySet(noLoginUserKey(clientIp, videoId));
    }

    private boolean trySet(String key) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", VIEW_TIME, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    public String loginUserKey(long userId, long videoId) {
        return "view-dedup:" + "user:" + userId + ":video:" + videoId;
    }

    public String noLoginUserKey(String clientIp, long videoId) {
        return "view-dedup:" + "userIp:" + clientIp + ":video:" + videoId;
    }

}
