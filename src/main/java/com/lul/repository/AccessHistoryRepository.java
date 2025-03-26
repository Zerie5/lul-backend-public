package com.lul.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lul.entity.AccessHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessHistoryRepository extends JpaRepository<AccessHistory, Long> {
    List<AccessHistory> findByUserIdOrderByLastAccessedDesc(Long userId);
    Optional<AccessHistory> findByUserIdAndCurrentSession(Long userId, boolean currentSession);
    @Modifying
    @Query(value = "UPDATE audit.access_history SET is_current_session = :isCurrentSession WHERE user_id = :userId", nativeQuery = true)
    void updateIsCurrentSessionByUserId(@Param("userId") Long userId, @Param("isCurrentSession") boolean isCurrentSession);
    boolean existsByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);
} 