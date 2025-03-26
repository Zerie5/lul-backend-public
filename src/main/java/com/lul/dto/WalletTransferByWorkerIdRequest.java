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
 * DTO for wallet-to-wallet transfers
 * This approach uses the receiver's worker ID instead of wallet ID for a more user-friendly experience
 * The sender only needs to know the recipient's worker ID (like an account number) and the currency to send
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransferByWorkerIdRequest {
    
    @NotNull(message = "Sender wallet type is required")
    private Integer senderWalletTypeId;  // This is the wallet_id from wallets table
    
    @NotBlank(message = "Receiver worker ID is required")
    private String receiverWorkerId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits in integer part and 2 digits in decimal part")
    private BigDecimal amount;
    
    @NotBlank(message = "PIN is required")
    private String pin;
    
    private String description;
    
    private String idempotencyKey;
} 