package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_agreements", schema = "auth")
@Data
public class UserAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "terms_id", nullable = false)
    private Long termsId;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;
} 