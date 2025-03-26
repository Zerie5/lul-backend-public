package com.lul.service;

import com.lul.entity.User;

/**
 * Service for PIN-related operations
 */
public interface PinService {
    
    /**
     * Verify if the provided PIN matches the user's stored PIN
     * 
     * @param user The user whose PIN to verify
     * @param pin The PIN to verify
     * @return true if the PIN is valid, false otherwise
     */
    boolean verifyPin(User user, String pin);
} 