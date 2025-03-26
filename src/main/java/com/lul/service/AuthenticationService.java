package com.lul.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lul.dto.LoginRequest;
import com.lul.dto.LoginResponse;
import com.lul.dto.base.DeviceInfoBase;
import com.lul.entity.User;
import com.lul.repository.UserRepository;

import com.lul.entity.UserToken;
import com.lul.repository.UserTokenRepository;
import com.lul.entity.AccessHistory;

import com.lul.dto.LocationInfo;

import com.lul.enums.RiskLevel;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import com.lul.constant.ErrorCode;
import com.lul.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

import com.lul.entity.FcmToken;
import com.lul.repository.FcmTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lul.dto.WalletInfoDto;
import com.lul.service.WalletService;

@Service
@Slf4j
@Transactional
public class AuthenticationService {
    
    @Autowired
    private AccessHistoryService accessHistoryService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserTokenRepository userTokenRepository;
    
    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private IpGeolocationService ipGeolocationService;
    
    @Autowired
    private DeviceFingerprintService deviceFingerprintService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private FcmTokenRepository fcmTokenRepository;
    
    @Autowired
    private WalletService walletService;
    
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
    
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
            
            // Generate fingerprint
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(request.getDeviceInfo());
            
            // Create new access history entry
            AccessHistory accessHistory = new AccessHistory();
            accessHistory.setUserId(user.getId());
            accessHistory.setDeviceName(request.getDeviceInfo().getDeviceName());
            accessHistory.setDeviceId(request.getDeviceInfo().getDeviceId());
            accessHistory.setOs(request.getDeviceInfo().getOs());
            accessHistory.setIpAddress(extractIpAddress(httpRequest));
            accessHistory.setDeviceFingerprint(deviceFingerprint);
            accessHistory.setRiskLevel(assessRiskLevel(user.getId(), deviceFingerprint));
            
            // Use the service to record access (handles location lookup, setting current session, etc.)
            accessHistoryService.recordAccess(accessHistory);
            
            // Get user profile data using existing service
            Map<String, Object> profileData = userProfileService.getUserProfile(user.getId());
            
            // Handle FCM token if provided during login
            if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
                try {
                    // Deactivate old tokens for this device
                    fcmTokenRepository.deactivateUserTokensForDevice(user.getId(), request.getDeviceInfo().getDeviceId());
                    
                    // Create and save new token
                    FcmToken fcmToken = new FcmToken();
                    fcmToken.setUserId(user.getId());
                    fcmToken.setToken(request.getFcmToken());
                    fcmToken.setDeviceId(request.getDeviceInfo().getDeviceId());
                    fcmToken.setActive(true);
                    fcmToken.setCreatedAt(LocalDateTime.now());
                    
                    fcmTokenRepository.save(fcmToken);
                    log.info("FCM token saved during login for user: {}, device: {}", user.getId(), request.getDeviceInfo().getDeviceId());
                } catch (Exception e) {
                    log.error("Failed to save FCM token during login", e);
                    // Don't fail login if token save fails
                }
            }
            
            // After successful login, send notification
            try {
                notificationService.sendNotification(
                    user.getId(),
                    "New Login",
                    "New login detected from " + request.getDeviceInfo().getDeviceName(),
                    Map.of(
                        "type", "security",
                        "event", "login",
                        "deviceId", request.getDeviceInfo().getDeviceId()
                    )
                );
            } catch (Exception e) {
                // Don't fail login if notification fails
                log.error("Failed to send login notification", e);
            }
            
            // Try to find an existing token for this user and device
            Optional<UserToken> existingToken = userTokenRepository.findByUserIdAndDeviceIdIgnoreActive(user.getId(), request.getDeviceInfo().getDeviceId());
            
            UserToken userToken;
            String jwtToken;
            
            if (existingToken.isPresent()) {
                // Use the existing token, just update its status and last used time
                userToken = existingToken.get();
                jwtToken = userToken.getToken();
                
                // Update token metadata
                userToken.setActive(true);
                userToken.setLastUsed(LocalDateTime.now());
                
                // Make sure the token hasn't expired
                if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                    // If expired, generate a new token value but keep the same record
                    jwtToken = jwtService.generateToken(Map.of(
                        "sub", user.getId().toString(),
                        "deviceId", request.getDeviceInfo().getDeviceId()
                    ));
                    userToken.setToken(jwtToken);
                    userToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
                }
            } else {
                // This is a new device for this user, create a new token
                jwtToken = jwtService.generateToken(Map.of(
                    "sub", user.getId().toString(),
                    "deviceId", request.getDeviceInfo().getDeviceId()
                ));
                
                // First deactivate any old tokens for this device (just in case)
                userTokenRepository.deactivateTokensForDevice(user.getId(), request.getDeviceInfo().getDeviceId());
                
                // Create a new token
                userToken = new UserToken();
                userToken.setUser(user);
                userToken.setToken(jwtToken);
                userToken.setDeviceId(request.getDeviceInfo().getDeviceId());
                userToken.setDeviceName(request.getDeviceInfo().getDeviceName());
                String fingerprint = deviceFingerprintService.generateFingerprint(request.getDeviceInfo());
                userToken.setDeviceFingerprint(fingerprint);
                userToken.setActive(true);
                userToken.setCreatedAt(LocalDateTime.now());
                userToken.setLastUsed(LocalDateTime.now());
                userToken.setFirstSeenAt(LocalDateTime.now());
                userToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
            }
            
            // Save the token (either updated existing or new)
            userTokenRepository.save(userToken);
            
            // Get user's wallet information
            List<WalletInfoDto> userWallets = walletService.getUserWalletsFormatted(user.getId());
            
            LoginResponse loginResponse = LoginResponse.builder()
                .status("success")
                .token(jwtToken)
                .userId(user.getUserWorkId())
                .profile(profileData)
                .registerStatus(user.getRegisterStatus())
                //.wallets(userWallets)
                .build();
            
            return loginResponse;
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new BaseException(ErrorCode.LOGIN_FAILED);
        }
    }

    private DeviceInfoBase extractDeviceInfo(HttpServletRequest request) {
        DeviceInfoBase info = new DeviceInfoBase();
        String userAgent = request.getHeader("User-Agent");
        info.setDeviceName(userAgent != null ? userAgent : "Unknown Device");
        info.setOs(parseOs(userAgent));
        return info;
    }

    private String parseOs(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "MacOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS")) return "iOS";
        return "Other";
    }

    private RiskLevel assessRiskLevel(Long userId, String deviceFingerprint) {
        return accessHistoryService.assessRiskLevel(userId, deviceFingerprint);
    }
} 