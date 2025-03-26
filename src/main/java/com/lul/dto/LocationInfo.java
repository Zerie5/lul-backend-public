package com.lul.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationInfo {
    private String city;
    private String country;
} 