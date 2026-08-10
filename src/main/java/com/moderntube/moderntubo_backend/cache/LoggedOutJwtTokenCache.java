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
package com.moderntube.moderntubo_backend.cache;

import com.moderntube.moderntubo_backend.event.OnUserLogoutSuccessEvent;
import com.moderntube.moderntubo_backend.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 토큰 정보 무효화
 * JWT 토큰은 변경 불가능하므로 토큰이 만료되지 않는 한 로그 아웃 후에도 계속 액세스 할 수 있다.
 * 이 캐시가 무기한 쌓이는 것을 방지하기 위해 최대 크기를 설정함
 * 각 토큰의 TTL은 만료 될 때까지 남아있는 시간 (초)입니다. 이는 JWT 토큰이 만료되면 어쨌든 사용할 수 없으므로 최적화로 수행됨
 */
@Component
@Slf4j
public class LoggedOutJwtTokenCache {

    private final StringRedisTemplate tokenEventMap;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public LoggedOutJwtTokenCache(StringRedisTemplate redisTemplate, JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        this.tokenEventMap = redisTemplate;
    }

    @EventListener
    public void onUserLogoutSuccess(OnUserLogoutSuccessEvent event) {
        markLogoutEventForToken(event);
    }

    public void markLogoutEventForToken(OnUserLogoutSuccessEvent event) {
        String token = event.getToken();
        if (Boolean.TRUE.equals(tokenEventMap.hasKey(token))) {
            log.info(String.format("Log out token for user [%s] is already present in the cache", event.getUserEmail()));

        } else {
            Date tokenExpiryDate = tokenProvider.getTokenExpiryFromJWT(token);
            long ttlForToken = getTTLForToken(tokenExpiryDate);
            log.info(String.format("Logout token cache set for [%s] with a TTL of [%s] seconds. Token is due expiry at [%s]", event.getUserEmail(), ttlForToken, tokenExpiryDate));
            tokenEventMap.opsForValue().set(token, event.getUserEmail(), ttlForToken, TimeUnit.SECONDS);
        }
    }

    public String getLoggedOutUserEmail(String token) {
        return tokenEventMap.opsForValue().get(token);
    }

    private long getTTLForToken(Date date) {
        long secondAtExpiry = date.toInstant().getEpochSecond();
        long secondAtLogout = Instant.now().getEpochSecond();
        return Math.max(0, secondAtExpiry - secondAtLogout);
    }
}
