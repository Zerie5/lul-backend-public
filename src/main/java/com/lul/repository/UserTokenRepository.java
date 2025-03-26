package com.lul.repository;

import com.lul.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByUserIdAndActiveTrue(Long userId);
    Optional<UserToken> findByTokenAndActiveTrue(String token);
    Optional<UserToken> findByUserIdAndDeviceIdAndActiveTrue(Long userId, String deviceId);
    
    List<UserToken> findAllByUserIdAndActiveTrue(Long userId);
    
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserToken t SET t.active = false WHERE t.user.id = :userId AND t.deviceId = :deviceId")
    void deactivateTokensForDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    
    // For backward compatibility
    default Optional<UserToken> findByUserId(Long userId) {
        return findByUserIdAndActiveTrue(userId);
    }
    
    // For backward compatibility
    default Optional<UserToken> findByToken(String token) {
        return findByTokenAndActiveTrue(token);
    }
    
    // For backward compatibility
    default Optional<UserToken> findByUserIdAndDeviceId(Long userId, String deviceId) {
        return findByUserIdAndDeviceIdAndActiveTrue(userId, deviceId);
    }
    
    // New method to find a token by user ID and device ID regardless of active status
    @Query("SELECT t FROM UserToken t WHERE t.user.id = :userId AND t.deviceId = :deviceId")
    Optional<UserToken> findByUserIdAndDeviceIdIgnoreActive(@Param("userId") Long userId, @Param("deviceId") String deviceId);
} 