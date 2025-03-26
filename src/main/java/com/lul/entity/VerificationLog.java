package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs", schema = "auth")
@Data
public class VerificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "attempt_time")
    private LocalDateTime attemptTime;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
} 