package com.converter.backend.exception;

import lombok.Getter;

/**
 * Exception thrown when user reaches plan limit
 * Includes upgrade info for better UX
 */
@Getter
public class LimitExceededException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String limitType; // "DAILY" or "MONTHLY"
    private final Integer currentUsage;
    private final Integer limitValue;
    private final String upgradePlanName;
    private final String upgradeUrl;
    
    public LimitExceededException(
        String message,
        String limitType,
        Integer currentUsage,
        Integer limitValue,
        String upgradePlanName,
        String upgradeUrl
    ) {
        super(message);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limitValue = limitValue;
        this.upgradePlanName = upgradePlanName;
        this.upgradeUrl = upgradeUrl;
    }
    
    /**
     * Simple constructor for backwards compatibility
     */
    public LimitExceededException(String message) {
        super(message);
        this.limitType = "UNKNOWN";
        this.currentUsage = null;
        this.limitValue = null;
        this.upgradePlanName = null;
        this.upgradeUrl = null;
    }
}
