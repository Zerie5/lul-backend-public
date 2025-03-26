package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens", schema = "auth")
@Data
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String token;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        expiresAt = LocalDateTime.now().plusMonths(6); // 6 months expiry
    }

    @PreUpdate
    public void validateToken() {
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("Invalid token format");
        }
    }
} 