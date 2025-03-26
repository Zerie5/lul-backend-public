package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "fee_types", schema = "wallet")
@Data
public class FeeType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "name", length = 50, nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
} 