package com.lul.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.lul.service.SmsService;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TestSmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send-sms")
    public ResponseEntity<?> sendTestSms(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Phone number is required"
                ));
            }
            
            String name = "Robel";
            boolean success = smsService.sendSms(
                phoneNumber,
                String.format("Hi %s, this is a test SMS from LulPay system!", name)
            );

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", success ? "success" : "error");
            responseMap.put("message", success ? "Test SMS sent successfully" : "Failed to send SMS");
            
            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to send test SMS: " + e.getMessage()
            ));
        }
    }
} 