package com.lul.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for non-wallet transfer responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonWalletTransferResponse {
    
    private String status;
    private Long transactionId;
    private Long senderWalletId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private String currency;
    private String description;
    private LocalDateTime timestamp;
    private BigDecimal senderWalletBalanceAfter;
    private String recipientName;
    private String recipientPhoneNumber;
    private Integer disbursementStageId;
    private String disbursementStageName;
} 