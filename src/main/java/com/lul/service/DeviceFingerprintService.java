package com.lul.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.lul.dto.base.DeviceInfoBase;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class DeviceFingerprintService {

    public String generateFingerprint(DeviceInfoBase deviceInfo) {
        try {
            // Use only existing fields
            String deviceData = String.format("%s|%s|%s",
                deviceInfo.getDeviceId(),
                deviceInfo.getDeviceName(),
                deviceInfo.getOs()
            );
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating device fingerprint", e);
            return null;
        }
    }
} 