package com.lul.service;

import com.lul.entity.User;
import com.lul.entity.UserWallet;
import com.lul.dto.WalletInfoDto;
import java.util.List;
import java.util.Map;

public interface WalletService {
    /**
     * Create default wallets for a new user
     * @param user The user for whom to create wallets
     * @return Map containing status and created wallet IDs
     */
    Map<String, Object> createDefaultWallets(User user);
    
    /**
     * Get all wallets for a user
     * @param userId The ID of the user
     * @return List of wallet information
     */
    List<UserWallet> getUserWallets(Long userId);
    
    /**
     * Get formatted wallet information for a user
     * @param userId The ID of the user
     * @return List of formatted wallet information
     */
    List<WalletInfoDto> getUserWalletsFormatted(Long userId);
    
    /**
     * Enable a specific wallet type for a user
     * @param userId The ID of the user
     * @param walletId The ID of the wallet type to enable
     * @return Map containing status and wallet information
     */
    Map<String, Object> enableWallet(Long userId, Integer walletId);
    
    /**
     * Get wallet ID for a specific country
     * @param countryName The name of the country
     * @return The wallet ID for the country, or null if not found
     */
    Integer getWalletIdForCountry(String countryName);
}
