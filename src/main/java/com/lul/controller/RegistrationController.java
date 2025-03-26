package com.lul.controller;


import com.lul.dto.UserRegistrationRequest;
import com.lul.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.Valid;
import java.util.Map;

import com.lul.constant.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lul.exception.RegistrationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import com.lul.service.EmailService;

import com.lul.service.JwtService;

import com.lul.repository.UserRepository;


@RestController
@RequestMapping("/api")
public class RegistrationController {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public RegistrationController(UserService userService, EmailService emailService, JwtService jwtService, UserRepository userRepository) {
        this.userService = userService;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            Map<String, Object> result = userService.registerUser(request);
            return ResponseEntity.ok(result);
        } catch (RegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "code", e.getErrorCode().getCode()
                ));
        } catch (Exception e) {
            logger.error("Unexpected error during registration: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.REGISTRATION_FAILED.getCode()
                ));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldError().getField();
        String message = ex.getBindingResult().getFieldError().getDefaultMessage();
        
        ErrorCode errorCode = switch (field) {
            case "password" -> {
                if (message.contains("at least 8 characters")) {
                    yield ErrorCode.PASSWORD_TOO_SHORT;  // ERR_304
                } else {
                    yield ErrorCode.PASSWORD_FORMAT;     // ERR_305
                }
            }
            case "email" -> ErrorCode.INVALID_EMAIL_FORMAT;
            case "phoneNumber" -> ErrorCode.INVALID_PHONE_FORMAT;
            case "username" -> ErrorCode.INVALID_USERNAME_FORMAT;
            default -> ErrorCode.SERVER_ERROR;
        };
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", "error",
                "code", errorCode.getCode()
            ));
    }
}

class AuthResponse {
    private String token;
    
    public AuthResponse(String token) {
        this.token = token;
    }
    
    // Getter
    public String getToken() {
        return token;
    }
}