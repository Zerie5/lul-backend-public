package com.lul.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String status;
    private String token;
    private String userId;
    private Map<String, Object> profile;
    private Integer registerStatus;
} 