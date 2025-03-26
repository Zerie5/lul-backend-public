package com.lul.service;

import com.lul.constant.ErrorCode;
import com.lul.exception.LulPayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending SMS messages to users.
 * This service handles the sending of SMS notifications to both wallet and non-wallet users.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    @Value("${sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${africastalking.url:}")
    private String africasTalkingUrl;

    @Value("${africastalking.apiKey:}")
    private String africasTalkingApiKey;
    
    @Value("${africastalking.username:}")
    private String africasTalkingUsername;

    /**
     * Sends an SMS message to the specified phone number.
     *
     * @param phoneNumber The recipient's phone number
     * @param message     The message content to send
     * @return true if the SMS was sent successfully, false otherwise
     */
    public boolean sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS sending is disabled. Would have sent to {}: {}", phoneNumber, message);
            return true;
        }

        try {
            log.info("Sending SMS to {}: {}", phoneNumber, message);
            
            // If we're in development or test environment, just log and return success
            if (africasTalkingUrl == null || africasTalkingUrl.isEmpty()) {
                log.info("Africa's Talking URL not configured. SMS would have been sent to {}: {}", phoneNumber, message);
                return true;
            }
            
            // Ensure phone number is in E.164 format (e.g., +256703859328)
            String formattedPhoneNumber = formatPhoneNumber(phoneNumber);
            log.info("Formatted phone number: {} -> {}", phoneNumber, formattedPhoneNumber);
            
            // Implementation for Africa's Talking
            HttpClient client = HttpClient.newHttpClient();
            
            // Prepare the JSON request body for bulk messaging endpoint according to documentation
            // The format should be:
            // {
            //   "username": "your_username",
            //   "phoneNumbers": ["+254711XXXYYY","+254733YYYZZZ"],
            //   "message": "Your message",
            //   "from": "your_sender_id"
            // }
            // Note: phoneNumbers must be a JSON array, not a string
            String jsonRequestBody = String.format(
                "{\"username\":\"%s\",\"phoneNumbers\":[\"%s\"],\"message\":\"%s\",\"from\":\"lul\"}",
                africasTalkingUsername,
                formattedPhoneNumber,
                message.replace("\"", "\\\"") // Escape any quotes in the message
            );
            
            log.info("Using sender ID: lul");
            
            // Log the full request details for troubleshooting
            log.info("Africa's Talking API request: URL={}, Username={}, PhoneNumbers=[{}], From=lul", 
                    africasTalkingUrl, africasTalkingUsername, formattedPhoneNumber);
            
            // Log partial API key for verification (first 10 chars only)
            if (africasTalkingApiKey != null && africasTalkingApiKey.length() > 10) {
                log.info("Using Africa's Talking API key starting with: {}", africasTalkingApiKey.substring(0, 10) + "...");
            } else {
                log.warn("Africa's Talking API key is null or too short");
            }
            
            // Log the full request body for troubleshooting
            log.info("Full JSON request body: {}", jsonRequestBody);
            
            // Create the HTTP request with proper headers for JSON
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(africasTalkingUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("apiKey", africasTalkingApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();
            
            log.info("Sending SMS request to Africa's Talking bulk messaging endpoint with JSON content type");
            
            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Log the full response for troubleshooting
            log.info("Africa's Talking API response: Status={}, Body={}", response.statusCode(), response.body());
            
            // Check if the request was successful
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            
            if (!success) {
                log.error("Failed to send SMS via Africa's Talking. Status code: {}, Response: {}", 
                        response.statusCode(), response.body());
                return false;
            }
            
            // Check if the response contains success indicators
            String responseBody = response.body();
            if (responseBody != null) {
                // Simple string check for success indicators in the response
                boolean apiSuccess = responseBody.contains("\"status\":\"success\"") || 
                                    responseBody.contains("\"Status\":\"Success\"") ||
                                    responseBody.contains("\"SMSMessageData\"");
                
                if (apiSuccess) {
                    log.info("SMS sent successfully via Africa's Talking to {}", formattedPhoneNumber);
                    return true;
                } else if (responseBody.contains("\"status\":\"error\"") || responseBody.contains("\"Status\":\"Failed\"")) {
                    log.error("Africa's Talking API returned error status in response: {}", responseBody);
                    return false;
                }
            }
            
            // If we can't determine from the response but the HTTP status was successful, assume success
            log.info("SMS sent successfully via Africa's Talking to {} (based on HTTP status)", formattedPhoneNumber);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            // Log the full stack trace for better troubleshooting
            e.printStackTrace();
            return false; // Don't throw exception, just return false to allow for retry
        }
    }
    
    /**
     * Formats a phone number to E.164 format
     * 
     * @param phoneNumber The phone number to format
     * @return The formatted phone number
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        
        // Remove any non-digit characters except the leading +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        // If the number doesn't start with +, add it
        if (!cleaned.startsWith("+")) {
            // If the number starts with 0, replace it with the country code
            if (cleaned.startsWith("0")) {
                cleaned = "+256" + cleaned.substring(1); // Assuming Uganda (+256)
            } else {
                cleaned = "+" + cleaned;
            }
        }
        
        return cleaned;
    }
} 