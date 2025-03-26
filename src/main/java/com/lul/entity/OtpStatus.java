package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "otp_statuses", schema = "auth")
@Data
public class OtpStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name")
    private String statusName;
} 