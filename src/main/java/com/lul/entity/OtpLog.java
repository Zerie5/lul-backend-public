package com.lul.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;



@Entity
@Table(name = "otp_logs", schema = "auth")
@Data
public class OtpLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status")
    private OtpStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "method")
    private OtpMethod method;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Column(name = "verified_at")
    private ZonedDateTime verifiedAt;
}

// Delete or comment out the enum definitions from OtpLog.java
/*
public enum OtpMethod {
    SMS, EMAIL
}

public enum OtpStatus {
    PENDING, VERIFIED, EXPIRED
}
*/ 