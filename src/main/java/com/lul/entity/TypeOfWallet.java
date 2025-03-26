package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "type_of_wallet", schema = "wallet")
@Data
public class TypeOfWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "type_name", length = 20, nullable = false, unique = true)
    private String typeName;
    
    @Column(name = "description")
    private String description;
} 