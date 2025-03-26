package com.lul.service.impl;

import com.lul.service.WalletService;
import com.lul.entity.User;
import com.lul.entity.UserWallet;
import com.lul.entity.Wallet;
import com.lul.repository.UserWalletRepository;
import com.lul.repository.WalletRepository;
import com.lul.repository.UserProfileRepository;
import com.lul.entity.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Optional;
import com.lul.dto.WalletInfoDto;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {

    private final UserWalletRepository userWalletRepository;
    private final WalletRepository walletRepository;
    private final UserProfileRepository userProfileRepository;
    
    // Country to wallet ID mapping
    private static final Map<String, Integer> COUNTRY_WALLET_MAP = new HashMap<>();
    
    static {
        COUNTRY_WALLET_MAP.put("Uganda", 1);      // UGX
        COUNTRY_WALLET_MAP.put("United States", 2); // USD
        COUNTRY_WALLET_MAP.put("Ethiopia", 3);    // ETB
        COUNTRY_WALLET_MAP.put("Kenya", 4);       // KES
        COUNTRY_WALLET_MAP.put("South Sudan", 5); // SSP
    }
    
    @Autowired
    public WalletServiceImpl(
            UserWalletRepository userWalletRepository,
            WalletRepository walletRepository,
            UserProfileRepository userProfileRepository) {
        this.userWalletRepository = userWalletRepository;
        this.walletRepository = walletRepository;
        this.userProfileRepository = userProfileRepository;
    }
    
    @Override
    @Transactional
    public Map<String, Object> createDefaultWallets(User user) {
        Map<String, Object> result = new HashMap<>();
        List<Integer> createdWalletIds = new ArrayList<>();
        
        try {
            // Always create USD wallet (ID 2)
            createUserWallet(user.getId(), 2);
            createdWalletIds.add(2);
            
            // Get user's country from profile
            Optional<UserProfile> profileOpt = userProfileRepository.findById(user.getId());
            if (profileOpt.isPresent()) {
                String userCountry = profileOpt.get().getCountry();
                
                // Create country-specific wallet based on user's country
                Integer localWalletId = getWalletIdForCountry(userCountry);
                if (localWalletId != null && localWalletId != 2) { // Skip if it's USD or not found
                    createUserWallet(user.getId(), localWalletId);
                    createdWalletIds.add(localWalletId);
                }
            }
            
            result.put("status", "success");
            result.put("walletIds", createdWalletIds);
            
            log.info("Created default wallets for user {}: {}", user.getId(), createdWalletIds);
            
        } catch (Exception e) {
            log.error("Failed to create default wallets for user {}: {}", user.getId(), e.getMessage());
            result.put("status", "error");
            result.put("message", "Failed to create wallets: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public List<UserWallet> getUserWallets(Long userId) {
        return userWalletRepository.findByUserId(userId);
    }
    
    @Override
    @Transactional
    public Map<String, Object> enableWallet(Long userId, Integer walletId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if wallet already exists for user
            if (userWalletRepository.existsByUserIdAndWalletId(userId, walletId)) {
                result.put("status", "error");
                result.put("message", "Wallet already exists for this user");
                return result;
            }
            
            // Check if wallet type exists
            if (!walletRepository.existsById(walletId)) {
                result.put("status", "error");
                result.put("message", "Invalid wallet type");
                return result;
            }
            
            // Create the wallet
            UserWallet userWallet = createUserWallet(userId, walletId);
            
            result.put("status", "success");
            result.put("message", "Wallet enabled successfully");
            result.put("walletId", userWallet.getId());
            
        } catch (Exception e) {
            log.error("Failed to enable wallet {} for user {}: {}", walletId, userId, e.getMessage());
            result.put("status", "error");
            result.put("message", "Failed to enable wallet: " + e.getMessage());
        }
        
        return result;
    }
    
    private UserWallet createUserWallet(Long userId, Integer walletId) {
        // Check if wallet already exists
        Optional<UserWallet> existingWallet = userWalletRepository.findByUserIdAndWalletId(userId, walletId);
        if (existingWallet.isPresent()) {
            return existingWallet.get();
        }
        
        // Create new wallet
        UserWallet userWallet = new UserWallet();
        userWallet.setUserId(userId);
        userWallet.setWalletId(walletId);
        userWallet.setBalance(BigDecimal.ZERO);
        userWallet.setPublicKey(generatePublicKey());
        
        return userWalletRepository.save(userWallet);
    }
    
    private String generatePublicKey() {
        // Generate a random UUID as a placeholder for blockchain public key
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Integer getWalletIdForCountry(String countryName) {
        if (countryName == null) {
            return null;
        }
        
        return COUNTRY_WALLET_MAP.getOrDefault(countryName, null);
    }

    @Override
    public List<WalletInfoDto> getUserWalletsFormatted(Long userId) {
        List<UserWallet> userWallets = userWalletRepository.findByUserId(userId);
        
        return userWallets.stream()
            .map(userWallet -> {
                Wallet wallet = userWallet.getWallet();
                
                // If wallet is not loaded via the relationship, try to load it directly
                if (wallet == null) {
                    Optional<Wallet> walletOpt = walletRepository.findById(userWallet.getWalletId());
                    if (walletOpt.isEmpty()) {
                        log.warn("Wallet not found for ID: {}", userWallet.getWalletId());
                        return null;
                    }
                    wallet = walletOpt.get();
                }
                
                return WalletInfoDto.builder()
                    .countryCode(wallet.getCountryCode().toLowerCase())
                    .name(wallet.getCurrencyName())
                    .description(wallet.getCurrencyName().replace("title", "icon"))
                    .availableBalance(userWallet.getBalance())
                    .code(wallet.getCurrencyCode())
                    .id(Long.valueOf(userWallet.getId()))
                    .walletTypeId(userWallet.getWalletId())
                    .build();
            })
            .filter(wallet -> wallet != null)
            .collect(Collectors.toList());
    }
}
