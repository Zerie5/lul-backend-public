package com.lul.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.lul.service.EmailService;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TestEmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping(value = "/send-email", consumes = "application/json")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Email is required"
                ));
            }

            emailService.sendEmail(
                email,
                "Test Email from LulPay",
                "This is a test email from LulPay system. If you received this, our email service is working correctly!"
            );

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test email sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to send test email: " + e.getMessage()
            ));
        }
    }
} 