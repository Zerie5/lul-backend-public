package com.lul.controller;

import com.lul.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for basic connectivity checks
 */
@RestController
@RequestMapping("/api/connectivity")
@RequiredArgsConstructor
@Slf4j
public class ConnectivityController {

    private final SmsService smsService;
    
    @Value("${africastalking.url:}")
    private String africasTalkingUrl;
    
    @Value("${africastalking.username:}")
    private String africasTalkingUsername;
    
    @Value("${africastalking.apiKey:}")
    private String africasTalkingApiKey;

    /**
     * Simple ping endpoint to check if the backend is up
     * 
     * @return A simple response indicating the backend is up
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Backend is running");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Detailed health check endpoint
     * 
     * @return Detailed system information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "LulPay Backend");
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Add system information
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        
        // Add memory information
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("maxMemory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
        memoryInfo.put("totalMemory", Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
        memoryInfo.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        
        response.put("system", systemInfo);
        response.put("memory", memoryInfo);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test SMS endpoint to verify SMS sending
     * 
     * @param phoneNumber The phone number to send the test SMS to
     * @return Response indicating whether the SMS was sent successfully
     */
    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, Object>> testSms(@RequestParam String phoneNumber) {
        log.info("Received request to send test SMS to: {}", phoneNumber);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("originalPhoneNumber", phoneNumber);
        
        // Add SMS configuration details
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("africasTalkingUrl", africasTalkingUrl);
        configInfo.put("africasTalkingUsername", africasTalkingUsername);
        // Only show first few characters of API key for security
        if (africasTalkingApiKey != null && africasTalkingApiKey.length() > 8) {
            configInfo.put("africasTalkingApiKey", africasTalkingApiKey.substring(0, 8) + "...");
        } else {
            configInfo.put("africasTalkingApiKey", "Not properly configured");
        }
        configInfo.put("senderId", "lul"); // Always using "lul" as sender ID
        configInfo.put("contentType", "application/json");
        configInfo.put("endpoint", "Bulk SMS Endpoint");
        configInfo.put("httpMethod", "HTTP POST");
        configInfo.put("requestFormat", "JSON with phoneNumbers as array");
        response.put("smsConfig", configInfo);
        
        try {
            // Format the phone number to ensure it's in the correct format
            String formattedPhoneNumber = phoneNumber;
            if (!phoneNumber.startsWith("+")) {
                if (phoneNumber.startsWith("0")) {
                    formattedPhoneNumber = "+256" + phoneNumber.substring(1);
                } else {
                    formattedPhoneNumber = "+" + phoneNumber;
                }
            }
            
            log.info("Sending test SMS to formatted number: {}", formattedPhoneNumber);
            
            // Add more detailed test message
            String testMessage = "This is a test SMS from LulPay. Timestamp: " + 
                                LocalDateTime.now().toString();
            
            // Enable detailed logging for this test
            log.info("Request details: URL={}, Username={}, PhoneNumbers=[{}], From=lul, Message='{}'", 
                    africasTalkingUrl, africasTalkingUsername, formattedPhoneNumber, testMessage);
            
            boolean sent = smsService.sendSms(formattedPhoneNumber, testMessage);
            
            if (sent) {
                response.put("status", "SUCCESS");
                response.put("message", "Test SMS sent successfully to " + formattedPhoneNumber);
                response.put("testMessage", testMessage);
                response.put("note", "Check the logs for detailed request and response information");
            } else {
                response.put("status", "FAILED");
                response.put("message", "Failed to send test SMS to " + formattedPhoneNumber);
                response.put("error", "SMS service returned false");
                response.put("troubleshooting", "Check the server logs for detailed error information");
            }
            
            response.put("formattedPhoneNumber", formattedPhoneNumber);
            response.put("testMessage", testMessage);
        } catch (Exception e) {
            log.error("Error sending test SMS: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Error sending test SMS: " + e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("stackTrace", e.getStackTrace()[0].toString());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check Africa's Talking configuration
     * 
     * @return Response with the current Africa's Talking configuration
     */
    @GetMapping("/check-at-config")
    public ResponseEntity<Map<String, Object>> checkAfricasTalkingConfig() {
        log.info("Checking Africa's Talking configuration");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Add Africa's Talking configuration details
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("url", africasTalkingUrl);
        configInfo.put("username", africasTalkingUsername);
        
        // Only show first few characters of API key for security
        if (africasTalkingApiKey != null && africasTalkingApiKey.length() > 8) {
            configInfo.put("apiKey", africasTalkingApiKey.substring(0, 8) + "...");
            configInfo.put("apiKeyConfigured", true);
        } else {
            configInfo.put("apiKey", "Not properly configured");
            configInfo.put("apiKeyConfigured", false);
        }
        
        // Check if all required configuration is present
        boolean isFullyConfigured = 
            africasTalkingUrl != null && !africasTalkingUrl.isEmpty() &&
            africasTalkingUsername != null && !africasTalkingUsername.isEmpty() &&
            africasTalkingApiKey != null && !africasTalkingApiKey.isEmpty();
        
        configInfo.put("isFullyConfigured", isFullyConfigured);
        configInfo.put("endpointType", "Bulk SMS Endpoint");
        configInfo.put("contentType", "application/json");
        configInfo.put("httpMethod", "HTTP POST");
        configInfo.put("requestFormat", "JSON with phoneNumbers as array");
        response.put("africasTalkingConfig", configInfo);
        
        // Add SMS configuration status
        response.put("status", isFullyConfigured ? "CONFIGURED" : "INCOMPLETE");
        response.put("message", isFullyConfigured ? 
            "Africa's Talking is properly configured" : 
            "Africa's Talking configuration is incomplete");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check authentication status
     * 
     * @return Response with authentication status and user information if authenticated
     */
    @GetMapping("/auth-check")
    public ResponseEntity<Map<String, Object>> checkAuthentication() {
        log.info("Checking authentication status");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Get authentication from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null) {
            response.put("isAuthenticated", authentication.isAuthenticated());
            response.put("principal", authentication.getPrincipal().toString());
            response.put("name", authentication.getName());
            response.put("authorities", authentication.getAuthorities().toString());
            response.put("details", authentication.getDetails() != null ? authentication.getDetails().toString() : "null");
            response.put("status", "SUCCESS");
            response.put("message", "Authentication information retrieved");
        } else {
            response.put("isAuthenticated", false);
            response.put("status", "WARNING");
            response.put("message", "No authentication information available");
        }
        
        return ResponseEntity.ok(response);
    }
} 