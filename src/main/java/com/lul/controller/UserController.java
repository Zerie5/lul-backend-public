package com.lul.controller;

import com.lul.entity.User;
import com.lul.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Valid;

import com.lul.dto.PinUpdateRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.lul.service.UserService;

import com.lul.dto.PinVerificationRequest;
import com.lul.dto.ProfileUpdateRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lul.repository.UserProfileRepository;
import com.lul.entity.UserToken;
import com.lul.repository.UserTokenRepository;
import jakarta.servlet.http.HttpServletRequest;

import com.lul.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;

import com.lul.service.WalletService;
import com.lul.entity.UserWallet;
import com.lul.dto.WalletInfoDto;

import java.util.Optional;

import com.lul.exception.NotFoundException;
import com.lul.constant.ErrorCode;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserProfileService userProfileService;
    private final WalletService walletService;

    @PostMapping("/user/pin/create")
    public ResponseEntity<?> createPin(@Valid @RequestBody PinUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            String hashedPin = passwordEncoder.encode(request.getPin());
            user.setPinHash(hashedPin);
            user.setRegisterStatus(4);  // Changed from setRegistrationStageId
            userRepository.save(user);
            
            // Create default wallets for the user
            Map<String, Object> walletResult = walletService.createDefaultWallets(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            if (walletResult.get("status").equals("success")) {
                response.put("wallets", walletResult.get("walletIds"));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("PIN creation failed: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "status", "error",
                    "code", "ERR_654"
                ));
        }
    }

    @PostMapping("/user/pin/update")
    public ResponseEntity<?> updatePin(@Valid @RequestBody PinUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            String hashedPin = passwordEncoder.encode(request.getPin());
            user.setPinHash(hashedPin);
            // Note: We don't update registration_stage_id here
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("PIN update failed: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "status", "error",
                    "code", "ERR_653"
                ));
        }
    }

    @GetMapping("/user/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            Map<String, Object> profileData = userProfileService.getUserProfile(user.getId());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", profileData
            ));
        } catch (Exception e) {
            logger.error("Profile fetch failed: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "status", "error",
                    "code", "ERR_503"  // Profile fetch failed
                ));
        }
    }

    @PostMapping("/user/pin/verify")
    public ResponseEntity<?> verifyPin(@Valid @RequestBody PinVerificationRequest request, 
                                     HttpServletRequest servletRequest) {
        try {
            logger.debug("Received PIN verification request for PIN: {}", request.getPin());

            // Development bypass check first
            if ("1988".equals(request.getPin())) {
                logger.debug("Using development bypass PIN");
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of("isValid", true)
                ));
            }

            // Get authenticated user and validate token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            String authHeader = servletRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "code", "ERR_502"
                ));
            }
            
            String providedToken = authHeader.substring(7);
            UserToken storedToken = userTokenRepository.findByToken(providedToken)
                .orElseThrow(() -> new RuntimeException("Token not found or inactive"));
                
            // No need to compare tokens since findByToken already ensures it's the correct token
            // Just verify that the token belongs to the current user
            if (!storedToken.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "code", "ERR_502"
                ));
            }
            
            // Verify PIN
            if (user.getPinHash() == null) {
                logger.debug("User has no PIN set");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "code", "ERR_651"
                ));
            }
            
            logger.debug("Stored PIN hash: {}", user.getPinHash());
            boolean isValid = passwordEncoder.matches(request.getPin(), user.getPinHash());
            logger.debug("PIN verification result: {}", isValid);
            
            if (!isValid) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "code", "ERR_651"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of("isValid", true)
            ));
        } catch (DataAccessException e) {
            // Database connectivity issues
            logger.error("Database error during PIN verification: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "code", "ERR_655"  // Using PIN_VERIFICATION_CONNECTIVITY_ERROR
            ));
        } catch (Exception e) {
            // Other unexpected errors
            logger.error("PIN verification failed: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "code", "ERR_651"  // Keep existing behavior for other exceptions
            ));
        }
    }

    @PutMapping("/user/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            return userService.updateProfile(user.getId().longValue(), request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "code", "ERR_503"));
        }
    }

    @PutMapping("/user/phone")
    public ResponseEntity<?> updatePhoneNumber(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody Map<String, String> request) {
        try {
            String newPhoneNumber = request.get("phoneNumber");
            if (newPhoneNumber == null || newPhoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "code", "ERR_301"));
            }
            
            Map<String, Object> result = userService.updatePhoneNumber(token, newPhoneNumber);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "code", "ERR_701"));
        }
    }

    @GetMapping("/user/wallets")
    public ResponseEntity<?> getUserWallets() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            List<WalletInfoDto> wallets = walletService.getUserWalletsFormatted(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", wallets);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get user wallets: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to retrieve wallets"
                ));
        }
    }

    @PostMapping("/user/wallets/enable")
    public ResponseEntity<?> enableWallet(@RequestBody Map<String, Integer> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            Integer walletId = request.get("walletId");
            if (walletId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Wallet ID is required"
                ));
            }
            
            Map<String, Object> result = walletService.enableWallet(user.getId(), walletId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to enable wallet: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to enable wallet"
                ));
        }
    }

    @GetMapping("/user/lookup/{userWorkId}")
    public ResponseEntity<?> getUserByWorkId(@PathVariable String userWorkId) {
        try {
            Map<String, Object> userData = userService.getUserByWorkId(userWorkId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", userData
            ));
        } catch (NotFoundException e) {
            // Use the existing USER_NOT_FOUND error code (ERR_501)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.USER_NOT_FOUND.getCode(),
                    "message", "User not found"
                ));
        } catch (DataAccessException e) {
            // Use the existing DATABASE_ERROR code (ERR_003)
            logger.error("Database error during user lookup: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.DATABASE_ERROR.getCode(),
                    "message", "Database error occurred"
                ));
        } catch (Exception e) {
            // Use the existing SERVER_ERROR code (ERR_002)
            logger.error("Error looking up user by work ID: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.SERVER_ERROR.getCode(),
                    "message", "Internal server error"
                ));
        }
    }
} 