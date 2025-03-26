package com.lul.controller;

import com.lul.dto.WalletInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestLoginResponseController {

    @GetMapping("/login-response")
    public ResponseEntity<?> getSampleLoginResponse() {
        // Create sample wallet data
        List<WalletInfoDto> wallets = new ArrayList<>();
        
        wallets.add(WalletInfoDto.builder()
            .countryCode("us")
            .name("dollartitle")
            .description("dollaricon")
            .availableBalance(new BigDecimal("10030.35"))
            .code("USD")
            .build());
            
        wallets.add(WalletInfoDto.builder()
            .countryCode("ug")
            .name("ugandanshillingtitle")
            .description("ugandanshillingicon")
            .availableBalance(new BigDecimal("5000000.00"))
            .code("UGX")
            .build());
            
        wallets.add(WalletInfoDto.builder()
            .countryCode("gb")
            .name("poundtitle")
            .description("poundicon")
            .availableBalance(new BigDecimal("7500.50"))
            .code("GBP")
            .build());
        
        // Create sample profile data
        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", "John");
        profile.put("lastName", "Doe");
        profile.put("email", "john.doe@example.com");
        profile.put("phone", "+1234567890");
        profile.put("country", "United States");
        
        // Create sample login response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        response.put("userId", "USR12345");
        response.put("profile", profile);
        response.put("registerStatus", 2);
        response.put("wallets", wallets);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/wallet-format")
    public ResponseEntity<?> getSampleWalletFormat() {
        // Create sample wallet data
        List<WalletInfoDto> wallets = new ArrayList<>();
        
        wallets.add(WalletInfoDto.builder()
            .countryCode("us")
            .name("dollartitle")
            .description("dollaricon")
            .availableBalance(new BigDecimal("10030.35"))
            .code("USD")
            .build());
            
        wallets.add(WalletInfoDto.builder()
            .countryCode("ug")
            .name("ugandanshillingtitle")
            .description("ugandanshillingicon")
            .availableBalance(new BigDecimal("5000000.00"))
            .code("UGX")
            .build());
            
        wallets.add(WalletInfoDto.builder()
            .countryCode("gb")
            .name("poundtitle")
            .description("poundicon")
            .availableBalance(new BigDecimal("7500.50"))
            .code("GBP")
            .build());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", wallets);
        
        return ResponseEntity.ok(response);
    }
} 