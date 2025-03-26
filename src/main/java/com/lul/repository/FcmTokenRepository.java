package com.lul.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lul.entity.FcmToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByUserIdAndDeviceIdAndActiveTrue(Long userId, String deviceId);
    List<FcmToken> findAllByUserIdAndActiveTrue(Long userId);
    
    Optional<FcmToken> findByUserIdAndActiveTrue(Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE FcmToken t SET t.active = false WHERE t.userId = :userId AND t.deviceId = :deviceId")
    void deactivateUserTokensForDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    
    @Modifying
    @Transactional
    @Query("UPDATE FcmToken f SET f.active = false WHERE f.userId = :userId")
    void deactivateUserTokens(@Param("userId") Long userId);
} 