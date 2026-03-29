package com.converter.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpgradePromptDto {
    
    // Error info
    private String error; // "LIMIT_REACHED"
    private String message; // "Daily limit reached (10/10)"
    
    // Limit details
    private String limitType; // "DAILY" or "MONTHLY"
    private Integer currentUsage; // Current usage
    private Integer limitValue; // Plan limit
    private String nextResetTime; // ISO timestamp when limit resets (e.g., tomorrow at 00:00)
    
    // Upgrade suggestion
    private String upgradePlanName; // "PRO" or next tier
    private UpgradePlanDetailsDto upgradePlan; // Current plan details
    private String upgradeUrl; // "/pricing?plan=PRO"
    
    // Additional context
    @Builder.Default
    private Boolean canUpgradeNow = true; // User can upgrade immediately
    @Builder.Default
    private String upgradeButtonText = "Upgrade to %s"; // "Upgrade to PRO"
}

/**
 * Details about the suggested upgrade plan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class UpgradePlanDetailsDto {
    private String name; // "PRO"
    private String description; // Plan description
    private Integer monthlyLimit; // "500"
    private Long monthlyPrice; // 4999 (cents, not dollars)
    private Long yearlyPrice;
}
