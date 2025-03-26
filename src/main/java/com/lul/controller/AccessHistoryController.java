package com.lul.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lul.entity.User;
import com.lul.service.AccessHistoryService;

@RestController
@RequestMapping("/api/access")
public class AccessHistoryController {

    private static final Logger log = LoggerFactory.getLogger(AccessHistoryController.class);

    @Autowired
    private AccessHistoryService accessHistoryService;

    @GetMapping("/history")
    public ResponseEntity<?> getAccessHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth.getPrincipal();
            
            Long userId;
            if (principal instanceof User) {
                userId = ((User) principal).getId();
                log.debug("Retrieving access history for user ID: {}", userId);
            } else if (principal instanceof String) {
                // If the principal is a string (username), we need to look up the user
                // This is a fallback and might not be needed if your authentication is set up correctly
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "code", "ERR_701",
                    "message", "User not properly authenticated"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "code", "ERR_701",
                    "message", "Unknown principal type: " + (principal != null ? principal.getClass().getName() : "null")
                ));
            }
            
            List<Map<String, Object>> history = accessHistoryService.getUserAccessHistory(userId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", history
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "code", "ERR_701",
                "message", "Failed to fetch access history: " + e.getMessage()
            ));
        }
    }
} 