package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", schema = "wallet")
@Data
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "rate", precision = 15, scale = 6, nullable = false)
    private BigDecimal rate;
    
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
    
    @Column(name = "from_wallet_id", nullable = false)
    private Integer fromWalletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id", insertable = false, updatable = false)
    private Wallet fromWallet;
    
    @Column(name = "to_wallet_id", nullable = false)
    private Integer toWalletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id", insertable = false, updatable = false)
    private Wallet toWallet;
} 