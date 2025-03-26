package com.lul.controller;

import com.lul.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import com.lul.dto.EmailRequest;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody EmailRequest request) {
        try {
            emailService.sendOtpEmail(request.getEmail(), request.getUserName());
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OTP sent successfully to " + request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }
} 