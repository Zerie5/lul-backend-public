package com.lul.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for wallet-to-wallet transfer using wallet IDs
 * @deprecated Use {@link WalletTransferByWorkerIdRequest} instead for a more user-friendly approach
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class WalletTransferRequest {
    
    @NotNull(message = "Sender wallet ID is required")
    private Long senderWalletId;
    
    @NotNull(message = "Receiver wallet ID is required")
    private Long receiverWalletId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits in integer part and 2 digits in decimal part")
    private BigDecimal amount;
    
    @NotBlank(message = "PIN is required")
    private String pin;
    
    private String description;
    
    // For idempotency
    private String idempotencyKey;
} 