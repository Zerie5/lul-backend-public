package com.lul.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for wallet transfer operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransferResponse {
    private String status;
    private Long transactionId;
    private Long senderWalletId;
    private Long receiverWalletId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private String currency;
    private String description;
    private LocalDateTime timestamp;
    private BigDecimal senderWalletBalanceAfter;
    private String receiverName;
} 