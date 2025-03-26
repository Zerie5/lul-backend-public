package com.lul.repository;

import com.lul.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    
    @Query("SELECT COUNT(v) FROM VerificationLog v WHERE v.userId = :userId " +
           "AND v.attemptTime > :since AND v.success = false")
    int countFailedAttemptsSince(
        @Param("userId") Long userId, 
        @Param("since") LocalDateTime since
    );
} 