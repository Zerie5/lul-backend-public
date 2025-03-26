package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets", schema = "wallet")
@Data
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "country_code", length = 10, nullable = false)
    private String countryCode;
    
    @Column(name = "currency_name", length = 50, nullable = false)
    private String currencyName;
    
    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "wallet_type_id", nullable = false)
    private Integer walletTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_type_id", insertable = false, updatable = false)
    private TypeOfWallet walletType;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
