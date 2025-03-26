package com.lul.repository;

import com.lul.entity.OtpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {
    
    @Query("SELECT o FROM OtpLog o WHERE o.userId = :userId " +
           "AND o.status.id = 1 AND o.attemptsCount < o.maxAttempts " +
           "ORDER BY o.sentAt DESC")
    Optional<OtpLog> findLatestPendingOtpByUserId(@Param("userId") Long userId);
    
    @Query("SELECT o FROM OtpLog o WHERE o.userId = :userId " +
           "AND o.otpCode = :otpCode AND o.status.id = 1")
    Optional<OtpLog> findPendingOtpByUserIdAndCode(
        @Param("userId") Long userId, 
        @Param("otpCode") String otpCode
    );

    @Modifying
    @Query("UPDATE OtpLog o SET o.attemptsCount = o.attemptsCount + 1 WHERE o.id = :otpId")
    void incrementAttemptCount(@Param("otpId") Long otpId);
} 