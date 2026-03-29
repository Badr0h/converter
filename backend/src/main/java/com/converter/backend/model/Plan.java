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
    private String name; // EX: "FREE", "PRO", "ENTERPRISE"

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

    // ===== NEW: AI & Cost Optimization Fields =====

    @Column(name = "ai_model", nullable = false)
    private String aiModel; // e.g., "gpt-5-nano", "gpt-5-mini", "gpt-5"

    @Column(name = "daily_limit")
    private Integer dailyLimit; // Daily request limit (for Free plan)

    @Column(name = "monthly_limit")
    private Integer monthlyLimit; // Monthly request limit (for Pro/Enterprise)

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate; // Plan-specific prompt template with placeholders

    @Column(name = "enable_caching", nullable = false)
    private Boolean enableCaching = true; // Enable result caching for this plan

    @Column(name = "cache_ttl_hours")
    private Integer cacheTtlHours = 24; // Cache time-to-live in hours

    // ===== NEW: Enterprise Plan Hard Limit =====
    @Column(name = "is_enterprise")
    private Boolean isEnterprise = false; // Marks as enterprise plan
    
    /**
     * Enterprise plans have a fixed hard limit (instead of unlimited).
     * Updated from "5000+ requests/month" to an actual number.
     * Default: 5000/month for Enterprise tier
     */
}
