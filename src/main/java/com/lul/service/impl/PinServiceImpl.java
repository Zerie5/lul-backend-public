package com.lul.service.impl;

import com.lul.entity.User;
import com.lul.service.PinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementation of the PinService interface
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PinServiceImpl implements PinService {

    private final PasswordEncoder passwordEncoder;
    
    /**
     * Verify if the provided PIN matches the user's stored PIN
     * 
     * @param user The user whose PIN to verify
     * @param pin The PIN to verify
     * @return true if the PIN is valid, false otherwise
     */
    @Override
    public boolean verifyPin(User user, String pin) {
        // Development bypass check first
        if ("1988".equals(pin)) {
            log.debug("Using development bypass PIN");
            return true;
        }
        
        // Check if user has a PIN set
        if (user.getPinHash() == null) {
            log.debug("User has no PIN set");
            return false;
        }
        
        // Verify PIN using password encoder
        log.debug("Verifying PIN for user ID: {}", user.getId());
        boolean isValid = passwordEncoder.matches(pin, user.getPinHash());
        log.debug("PIN verification result: {}", isValid);
        
        return isValid;
    }
} 