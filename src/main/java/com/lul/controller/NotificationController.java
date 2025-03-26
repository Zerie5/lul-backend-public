package com.lul.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import com.lul.entity.FcmToken;
import com.lul.repository.FcmTokenRepository;
import com.lul.service.NotificationService;
import com.lul.service.JwtService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    /**
     * Updates the FCM token for a user.
     * 
     * Note: For optimal notification delivery, it's recommended to include the FCM token
     * directly in the login request using the LoginRequest.fcmToken field. This ensures
     * that the token is registered before any notifications are sent.
     * 
     * This endpoint can still be used for token updates that occur after login
     * (e.g., when Firebase refreshes the token).
     */
    @PostMapping("/fcm-token")
    @Transactional
    public ResponseEntity<Map<String, String>> updateFcmToken(@RequestHeader("Authorization") String authHeader,
                                                         @RequestBody Map<String, String> requestBody) {
        try {
            String token = requestBody.get("token");
            String deviceId = requestBody.getOrDefault("deviceId", "default-device");
            
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "FCM token is required"));
            }
            
            // Extract user ID from JWT token
            String jwt = authHeader.substring(7); // Remove "Bearer " prefix
            String userIdStr = jwtService.extractUserId(jwt);
            Long userId = Long.parseLong(userIdStr); // Convert to Long instead of UUID
            
            // Deactivate old tokens for this device
            fcmTokenRepository.deactivateUserTokensForDevice(userId, deviceId);
            
            // Create and save new token
            FcmToken fcmToken = new FcmToken();
            fcmToken.setUserId(userId);
            fcmToken.setToken(token);
            fcmToken.setDeviceId(deviceId);
            fcmToken.setActive(true);
            fcmToken.setCreatedAt(LocalDateTime.now());
            
            fcmTokenRepository.save(fcmToken);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "FCM token updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update FCM token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to update FCM token"));
        }
    }
    
    @PostMapping("/test-notification")
    public ResponseEntity<Map<String, String>> testNotification(@RequestHeader("Authorization") String authHeader,
                                                           @RequestBody Map<String, String> requestBody) {
        try {
            // Extract user ID from JWT token
            String jwt = authHeader.substring(7); // Remove "Bearer " prefix
            String userIdStr = jwtService.extractUserId(jwt);
            Long userId = Long.parseLong(userIdStr);
            
            String title = requestBody.getOrDefault("title", "Test Notification");
            String body = requestBody.getOrDefault("body", "This is a test notification from LulPay");
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", "test");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Send notification
            notificationService.sendNotification(userId, title, body, data);
            
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Test notification sent successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to send test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to send test notification: " + e.getMessage()));
        }
    }
    
    /**
     * Provides documentation for frontend developers on how to implement FCM token handling.
     * This is just for documentation purposes and doesn't actually do anything.
     */
    @GetMapping("/fcm-docs")
    public ResponseEntity<Map<String, Object>> getFcmDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // Overview
        docs.put("overview", "This document specifies the data structures expected by the backend for FCM token handling.");
        
        // Login Request with FCM Token
        Map<String, Object> loginRequestSpec = new HashMap<>();
        loginRequestSpec.put("endpoint", "/api/auth/login");
        loginRequestSpec.put("method", "POST");
        loginRequestSpec.put("contentType", "application/json");
        
        Map<String, Object> loginRequestStructure = new HashMap<>();
        loginRequestStructure.put("email", "string (required) - User's email address");
        loginRequestStructure.put("password", "string (required) - User's password");
        
        Map<String, Object> deviceInfoStructure = new HashMap<>();
        deviceInfoStructure.put("deviceId", "string (required) - Unique identifier for the device");
        deviceInfoStructure.put("deviceName", "string (required) - Human-readable name of the device");
        deviceInfoStructure.put("os", "string (required) - Operating system of the device");
        deviceInfoStructure.put("ipAddress", "string (optional) - IP address of the device");
        
        loginRequestStructure.put("deviceInfo", deviceInfoStructure);
        loginRequestStructure.put("fcmToken", "string (optional) - Firebase Cloud Messaging token for the device");
        
        loginRequestSpec.put("requestStructure", loginRequestStructure);
        docs.put("loginRequest", loginRequestSpec);
        
        // FCM Token Update Request
        Map<String, Object> tokenUpdateSpec = new HashMap<>();
        tokenUpdateSpec.put("endpoint", "/api/notifications/fcm-token");
        tokenUpdateSpec.put("method", "POST");
        tokenUpdateSpec.put("contentType", "application/json");
        tokenUpdateSpec.put("authorization", "Bearer JWT-TOKEN");
        
        Map<String, Object> tokenUpdateStructure = new HashMap<>();
        tokenUpdateStructure.put("token", "string (required) - The new FCM token");
        tokenUpdateStructure.put("deviceId", "string (required) - The device ID associated with this token");
        
        tokenUpdateSpec.put("requestStructure", tokenUpdateStructure);
        docs.put("fcmTokenUpdate", tokenUpdateSpec);
        
        // Test Notification Request
        Map<String, Object> testNotificationSpec = new HashMap<>();
        testNotificationSpec.put("endpoint", "/api/notifications/test-notification");
        testNotificationSpec.put("method", "POST");
        testNotificationSpec.put("contentType", "application/json");
        testNotificationSpec.put("authorization", "Bearer JWT-TOKEN");
        
        Map<String, Object> testNotificationStructure = new HashMap<>();
        testNotificationStructure.put("title", "string (optional) - Title of the notification, defaults to 'Test Notification'");
        testNotificationStructure.put("body", "string (optional) - Body of the notification, defaults to 'This is a test notification from LulPay'");
        
        testNotificationSpec.put("requestStructure", testNotificationStructure);
        docs.put("testNotification", testNotificationSpec);
        
        return ResponseEntity.ok(docs);
    }
} 