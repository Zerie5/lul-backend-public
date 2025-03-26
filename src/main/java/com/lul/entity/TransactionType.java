package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "transaction_types", schema = "wallet")
@Data
public class TransactionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "type_name", length = 50, nullable = false, unique = true)
    private String typeName;
    
    @Column(name = "language_string", length = 50)
    private String languageString;
    
    @Column(name = "otp_required")
    private Boolean otpRequired = false;
    
    @Column(name = "is_reversal_allowed")
    private Boolean isReversalAllowed = true;
} 