package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "otp_methods", schema = "auth")
@Data
public class OtpMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "method_name")
    private String methodName;
} 