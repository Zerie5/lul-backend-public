package com.lul.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletInfoDto {
    private String countryCode;
    private String name;
    private String description;
    private BigDecimal availableBalance;
    private String code;
    private Long id;
    private Integer walletTypeId;
} 