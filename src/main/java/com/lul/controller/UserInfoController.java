package com.lul.controller;

import com.lul.entity.User;
import com.lul.repository.UserRepository;
import com.lul.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for retrieving user information from JWT token
 */
@RestController
@RequestMapping("/api/user-info")
@RequiredArgsConstructor
@Slf4j
public class UserInfoController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Get user ID and worker ID from JWT token
     * 
     * @return User ID and worker ID
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo() {
        log.info("Received request to get current user info");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        
        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                // Get user ID from authentication
                String username = authentication.getName();
                log.info("Authenticated user: {}", username);
                
                // Try to find user in database by email first
                User user = userRepository.findByEmail(username)
                    .orElse(null);
                
                if (user != null) {
                    log.info("Found user in database: ID={}, Email={}", user.getId(), user.getEmail());
                    
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getId());
                    userInfo.put("workerId", user.getUserWorkId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("firstName", user.getFirstName());
                    userInfo.put("lastName", user.getLastName());
                    userInfo.put("phoneNumber", user.getPhoneNumber());
                    
                    response.put("status", "SUCCESS");
                    response.put("message", "User information retrieved successfully");
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
                log.warn("User not authenticated or authentication is null");
                response.put("status", "ERROR");
                response.put("message", "User not authenticated");
            }
        } catch (Exception e) {
            log.error("Error retrieving user info: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Error retrieving user information: " + e.getMessage());
            response.put("errorType", e.getClass().getName());
        }
        
        // Log the final response object
        log.info("Final response: status={}, message={}, hasUserInfo={}", 
                response.get("status"), 
                response.get("message"), 
                response.containsKey("userInfo"));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple endpoint to get just the user ID and worker ID from JWT token
     * This endpoint directly extracts the user ID from the token and looks up the worker ID
     * 
     * @param authHeader The Authorization header containing the JWT token
     * @return User ID and worker ID
     */
    @GetMapping("/worker-id")
    public ResponseEntity<Map<String, Object>> getWorkerIdFromToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.info("Received request to get worker ID from token");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No token provided or invalid token format");
                response.put("status", "ERROR");
                response.put("message", "No token provided or invalid token format");
                return ResponseEntity.ok(response);
            }
            
            // Extract token from header
            String token = authHeader.substring(7);
            
            // Validate token
            boolean isValid = jwtService.validateToken(token);
            
            if (isValid) {
                // Extract user ID directly from token
                String userIdStr = jwtService.extractUserId(token);
                log.info("Extracted user ID from token: {}", userIdStr);
                
                try {
                    Long userId = Long.parseLong(userIdStr);
                    
                    // Find user by ID
                    User user = userRepository.findById(userId).orElse(null);
                    
                    if (user != null) {
                        log.info("Found user in database: ID={}, WorkID={}", user.getId(), user.getUserWorkId());
                        
                        response.put("status", "SUCCESS");
                        response.put("message", "Worker ID retrieved successfully");
                        response.put("userId", user.getId());
                        response.put("workerId", user.getUserWorkId());
                        
                        log.info("Sending worker ID info: userId={}, workerId={}", 
                                user.getId(), user.getUserWorkId());
                    } else {
                        log.warn("User not found in database for ID: {}", userId);
                        response.put("status", "ERROR");
                        response.put("message", "User not found in database");
                    }
                } catch (NumberFormatException e) {
                    log.error("Error parsing user ID from token: {}", e.getMessage());
                    response.put("status", "ERROR");
                    response.put("message", "Invalid user ID in token");
                }
            } else {
                log.warn("Invalid token provided");
                response.put("status", "ERROR");
                response.put("message", "Invalid token");
            }
        } catch (Exception e) {
            log.error("Error retrieving worker ID: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "Error retrieving worker ID: " + e.getMessage());
            response.put("errorType", e.getClass().getName());
        }
        
        return ResponseEntity.ok(response);
    }
} 