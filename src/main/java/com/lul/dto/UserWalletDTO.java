package com.lul.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for user wallet information to be included in the login response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWalletDTO {
    private String countryCode;
    private String name;
    private String description;
    private BigDecimal availableBalance;
    private String code;
} 