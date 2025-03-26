package com.lul.controller;

import com.lul.dto.WalletInfoDto;
import com.lul.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lul.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestWalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/wallets")
    public ResponseEntity<?> testWalletFormat() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            List<WalletInfoDto> wallets = walletService.getUserWalletsFormatted(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", wallets);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve wallet information: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 