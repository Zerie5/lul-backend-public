package com.lul.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.persistence.EnumType;
import com.lul.enums.RiskLevel;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "access_history", schema = "audit")
@Data
public class AccessHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "access_time")
    private LocalDateTime accessTime;

    @Column(name = "os", length = 50, nullable = false)
    private String os;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "is_current_session")
    private boolean currentSession;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;
} 