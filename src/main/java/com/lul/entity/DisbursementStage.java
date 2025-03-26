package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing disbursement stages
 */
@Entity
@Table(name = "disbursement_stages", schema = "wallet")
@Data
public class DisbursementStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "stage_name", length = 50, nullable = false, unique = true)
    private String stageName;
    
    @Column(name = "description")
    private String description;
}
