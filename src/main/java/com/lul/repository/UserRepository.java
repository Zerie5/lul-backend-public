package com.lul.repository;

import com.lul.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByUserWorkId(String userWorkId);
    Optional<User> findByUserWorkId(String userWorkId);
    @Query("SELECT u FROM User u " +
           "JOIN UserToken t ON u = t.user " +
           "JOIN UserProfile p ON u = p.user " +
           "WHERE t.token = :token")
    Optional<User> findByToken(@Param("token") String token);
    Optional<User> findByEmail(String email);
    
    /**
     * Count users with updated_at timestamp after the given date
     * This is used to determine active users for the dashboard
     * 
     * @param cutoffDate The cutoff date for considering a user active
     * @return Count of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt > :cutoffDate")
    long countByUpdatedAtAfter(@Param("cutoffDate") LocalDateTime cutoffDate);
} 