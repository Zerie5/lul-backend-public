package com.lul.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletDTO {
    private Integer id;
    private Integer walletId;
    private BigDecimal balance;
    private String publicKey;
    private String currencyCode;
    private String currencyName;
    private String countryCode;
    private String description;
}
