package com.lul.controller;

import com.lul.entity.User;
import com.lul.repository.UserRepository;
import com.lul.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for checking authentication and retrieving user information
 */
@RestController
@RequestMapping("/api/auth-check")
@RequiredArgsConstructor
@Slf4j
public class AuthCheckController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Check JWT token and return user information
     * 
     * @param authHeader The Authorization header containing the JWT token
     * @return User information if the token is valid
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request to verify token");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No token provided or invalid token format: {}", authHeader == null ? "null" : authHeader);
                response.put("status", "ERROR");
                response.put("message", "No token provided or invalid token format");
                response.put("isValid", false);
                return ResponseEntity.ok(response);
            }
            
            // Extract token from header
            String token = authHeader.substring(7);
            log.debug("Extracted token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            
            // Validate token
            boolean isValid = jwtService.validateToken(token);
            response.put("isValid", isValid);
            log.info("Token validation result: {}", isValid);
            
            if (isValid) {
                // Get username from token
                String username = jwtService.extractUserId(token);
                log.info("Token is valid for user: {}", username);
                
                // Find user in database
                Optional<User> userOpt = userRepository.findByEmail(username);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info("Found user in database: ID={}, Email={}", user.getId(), user.getEmail());
                    
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getId());
                    userInfo.put("workerId", user.getUserWorkId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("firstName", user.getFirstName());
                    userInfo.put("lastName", user.getLastName());
                    
                    response.put("status", "SUCCESS");
                    response.put("message", "Token is valid");
                    response.put("userInfo", userInfo);
                    
                    // Log the complete response for debugging
                    log.info("Sending response with user info: userId={}, workerId={}, email={}", 
                            user.getId(), user.getUserWorkId(), user.getEmail());
                } else {
                    log.warn("User not found in database for username: {}", username);
                    response.put("status", "ERROR");
                    response.put("message", "User not found in database");
                }
            } else {
                log.warn("Token is invalid");
                response.put("status", "ERROR");
                response.put("message", "Invalid token");
            }
        } catch (Exception e) {
            log.error("Error verifying token: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Error verifying token: " + e.getMessage());
            response.put("isValid", false);
            response.put("errorType", e.getClass().getName());
        }
        
        // Log the final response object
        log.info("Final response: status={}, message={}, isValid={}, hasUserInfo={}", 
                response.get("status"), 
                response.get("message"), 
                response.get("isValid"),
                response.containsKey("userInfo"));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple endpoint to check if the auth-check service is up
     * 
     * @return A simple response indicating the service is up
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Auth check service is running");
        response.put("timestamp", LocalDateTime.now().toString());
        
        log.info("Auth check ping endpoint called, responding with status UP");
        return ResponseEntity.ok(response);
    }
} 