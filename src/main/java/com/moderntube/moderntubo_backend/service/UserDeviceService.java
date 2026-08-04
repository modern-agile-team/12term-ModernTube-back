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

import com.moderntube.moderntubo_backend.exception.TokenRefreshException;
import com.moderntube.moderntubo_backend.model.UserDevice;
import com.moderntube.moderntubo_backend.model.payload.DeviceInfo;
import com.moderntube.moderntubo_backend.model.token.RefreshToken;
import com.moderntube.moderntubo_backend.repository.UserDeviceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * 사용자 id로 디바이스 찾기
     * @param userId
     * @return
     */
    public Optional<UserDevice> findByUserId(Long userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    /**
     * 사용자 ID, 장치 ID로 찾기
     * @param userId
     * @param deviceId
     * @return
     */
    public Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId) {
        return userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    public Optional<UserDevice> findByUserIDAndDeviceId(Long userId, String deviceId) {
        Optional<UserDevice> result = userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId);
        if (result.isPresent()) {
            refreshTokenService.increaseCount(result.get().getRefreshToken());
            return result;
        } else {
            return Optional.empty();
        }
    }

    public List<UserDevice> findAllByUserId(Long userId) {
        return userDeviceRepository.findAllByUserId(userId);
    }

    /**
     * refresh token으로 디바이스 찾기
     * @param refreshToken
     * @return
     */
    public Optional<UserDevice> findByRefreshToken(RefreshToken refreshToken) {
        return userDeviceRepository.findByRefreshToken(refreshToken);
    }

    /**
     * 새 사용자 디바이스를 만들고 사용자를 현재 장치로 설정
     * @param deviceInfo
     * @return
     */
    public UserDevice createUserDevice(DeviceInfo deviceInfo) {
        UserDevice userDevice = new UserDevice();
        userDevice.setDeviceId(deviceInfo.getDeviceId());
        userDevice.setDeviceType(deviceInfo.getDeviceType());
        userDevice.setNotificationToken(deviceInfo.getNotificationToken());
        userDevice.setRefreshActive(true);
        return userDevice;
    }

    /**
     * 토큰에 해당하는 사용자 장치에 새로 고침이 활성화되어 있는지 확인하고 클라이언트에 적절한 오류를 발생시킴
     * @param refreshToken
     */
    void verifyRefreshAvailability(RefreshToken refreshToken) {
        UserDevice userDevice = findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException(refreshToken.getToken(), "No device found for the matching token. Please login again"));

        if (!userDevice.getRefreshActive()) {
            throw new TokenRefreshException(refreshToken.getToken(), "Refresh blocked for the device. Please login through a different device");
        }
    }

    public void save(UserDevice userDevice) {
        userDeviceRepository.save(userDevice);
    }
}
