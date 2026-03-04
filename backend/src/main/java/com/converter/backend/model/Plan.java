package com.converter.backend.model;

import java.math.BigDecimal;

import jakarta.persistence.*;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // includes getter + setter + toString + equals + hashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // EX: "STARTER", "PROFESSIONAL", "ENTERPRISE"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Integer duration; // en jours, mois, etc.

    @Column(name = "monthly_price")
    private BigDecimal monthlyPrice;

    @Column(name = "annual_price")
    private BigDecimal annualPrice;

    @Column(name = "max_conversions")
    private Integer maxConversions; // Maximum conversions per month for this plan
}
