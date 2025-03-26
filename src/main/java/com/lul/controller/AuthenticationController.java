package com.lul.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lul.service.AuthenticationService;
import com.lul.service.UserService;
import com.lul.dto.LoginRequest;
import com.lul.dto.LoginResponse;
import com.lul.dto.UserRegistrationRequest;
import com.lul.exception.RegistrationException;
import com.lul.constant.ErrorCode;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authenticationService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "code", "ERR_401",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            Map<String, Object> response = userService.registerUser(request);
            
            // Check if the response contains an error
            if (response.containsKey("status") && "error".equals(response.get("status"))) {
                String errorCode = (String) response.get("code");
                
                // Set appropriate HTTP status based on error code
                if (errorCode != null) {
                    if (errorCode.equals(ErrorCode.DUPLICATE_USERNAME.getCode()) || 
                        errorCode.equals(ErrorCode.DUPLICATE_EMAIL.getCode()) ||
                        errorCode.equals(ErrorCode.DUPLICATE_PHONE.getCode())) {
                        // Return 409 Conflict for duplicate resources
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                    }
                }
                
                // Default to BAD_REQUEST for other errors
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed with exception", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.REGISTRATION_FAILED.getCode(),
                    "message", "Registration failed due to an unexpected error"
                ));
        }
    }
} 