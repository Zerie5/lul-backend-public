package com.lul.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for non-wallet transfer requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonWalletTransferRequest {
    
    @NotNull(message = "Sender wallet type ID is required")
    private Integer senderWalletTypeId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits in integer part and 2 digits in fraction part")
    private BigDecimal amount;
    
    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 characters")
    private String pin;
    
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;
    
    @Size(max = 50, message = "Idempotency key must be at most 50 characters")
    private String idempotencyKey;
    
    // Recipient details
    @NotBlank(message = "Recipient full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String recipientFullName;
    
    @NotBlank(message = "ID document type is required")
    @Size(max = 50, message = "ID document type must be at most 50 characters")
    private String idDocumentType;
    
    @NotBlank(message = "ID number is required")
    @Size(max = 50, message = "ID number must be at most 50 characters")
    private String idNumber;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phoneNumber;
    
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;
    
    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country must be at most 50 characters")
    private String country;
    
    @Size(max = 50, message = "State must be at most 50 characters")
    private String state;
    
    @Size(max = 50, message = "City must be at most 50 characters")
    private String city;
    
    @Size(max = 50, message = "Relationship must be at most 50 characters")
    private String relationship;
} 