package com.converter.backend.service;

import com.converter.backend.model.MonthlyConversionUsage;
import com.converter.backend.model.User;
import com.converter.backend.repository.MonthlyConversionUsageRepository;
import com.converter.backend.repository.ConversionRepository;
import com.converter.backend.exception.LimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * UsageLimiter Service
 * 
 * Tracks and enforces usage limits for each subscription plan:
 * - FREE: Daily limit (e.g., 7 requests/day)
 * - PRO: Monthly limit (e.g., 500 requests/month)
 * - ENTERPRISE: High monthly limit (e.g., 5000+ requests/month)
 * 
 * Features:
 * - Track daily usage (for Free plan)
 * - Track monthly usage (for Pro & Enterprise)
 * - Enforce hard limits with clear error messages
 * - Reset counters automatically
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageLimiter {

    private final MonthlyConversionUsageRepository monthlyUsageRepository;
    private final ConversionRepository conversionRepository;

    /**
     * Check if user can perform a conversion request
     * 
     * @param user The user
     * @param planName The subscription plan: "FREE", "PRO", "ENTERPRISE"
     * @param dailyLimit Daily limit (only for FREE plan)
     * @param monthlyLimit Monthly limit
     * @throws UsageLimitExceededException if user has exceeded limits
     */
    public void checkUsageLimit(User user, String planName, Integer dailyLimit, Integer monthlyLimit) {
        if (user == null || planName == null) {
            throw new IllegalArgumentException("User and plan name are required");
        }

        log.debug("Checking usage limits for user: {} | Plan: {}", user.getId(), planName);

        switch (planName.toUpperCase()) {
            case "FREE" -> checkDailyLimit(user, dailyLimit);
            case "PRO", "ENTERPRISE" -> checkMonthlyLimit(user, monthlyLimit);
            default -> throw new IllegalArgumentException("Unknown plan: " + planName);
        }
    }

    /**
     * Check daily usage limit (FREE plan only)
     */
    private void checkDailyLimit(User user, Integer dailyLimit) {
        if (dailyLimit == null || dailyLimit <= 0) {
            return; // No limit
        }

        long todayConversions = conversionRepository.countByUserAndCreatedAtAfter(
            user,
            LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        );

        if (todayConversions >= dailyLimit) {
            log.warn("User {} exceeded daily limit: {}/{}", user.getId(), todayConversions, dailyLimit);
            
            throw new LimitExceededException(
                String.format("Daily limit exceeded (%d/%d). Upgrade to PRO for unlimited conversions.", todayConversions, dailyLimit),
                "DAILY",
                (int) todayConversions,
                dailyLimit,
                "PRO",
                "/pricing?plan=PRO"
            );
        }

        log.debug("Daily limit OK for user {}: {}/{}", user.getId(), todayConversions, dailyLimit);
    }

    /**
     * Check monthly usage limit (PRO & ENTERPRISE)
     */
    private void checkMonthlyLimit(User user, Integer monthlyLimit) {
        if (monthlyLimit == null || monthlyLimit <= 0) {
            return; // No limit (unlimited enterprise)
        }

        long monthlyConversions = getMonthlyConversionCount(user);

        if (monthlyConversions >= monthlyLimit) {
            log.warn("User {} exceeded monthly limit: {}/{}", user.getId(), monthlyConversions, monthlyLimit);
            throw new LimitExceededException(
                String.format("Monthly limit exceeded (%d/%d). Upgrade to ENTERPRISE for higher limits.", monthlyConversions, monthlyLimit),
                "MONTHLY",
                (int) monthlyConversions,
                (int) monthlyLimit,
                "ENTERPRISE",
                "/pricing?plan=ENTERPRISE"
            );
        }

        log.debug("Monthly limit OK for user {}: {}/{}", user.getId(), monthlyConversions, monthlyLimit);
    }

    /**
     * Record a conversion request (increment usage counters)
     * 
     * @param user The user
     */
    @Transactional
    public void recordConversion(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Get or create monthly usage record
        MonthlyConversionUsage usage = monthlyUsageRepository.findByUserAndYearMonth(user, yearMonth)
            .orElseGet(() -> {
                MonthlyConversionUsage newUsage = new MonthlyConversionUsage();
                newUsage.setUser(user);
                newUsage.setYearMonth(yearMonth);
                newUsage.setConversionCount(0);
                return newUsage;
            });

        usage.setConversionCount(usage.getConversionCount() + 1);
        monthlyUsageRepository.save(usage);

        log.debug("Recorded conversion for user: {} | Monthly count: {}", user.getId(), usage.getConversionCount());
    }

    /**
     * Get current month's conversion count
     * 
     * @param user The user
     * @return Number of conversions in current month
     */
    public long getMonthlyConversionCount(User user) {
        if (user == null) {
            return 0;
        }

        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return monthlyUsageRepository.findByUserAndYearMonth(user, yearMonth)
            .map(MonthlyConversionUsage::getConversionCount)
            .map(Long::valueOf)
            .orElse(0L);
    }

    /**
     * Get current day's conversion count
     * 
     * @param user The user
     * @return Number of conversions today
     */
    public long getDailyConversionCount(User user) {
        if (user == null) {
            return 0;
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return conversionRepository.countByUserAndCreatedAtAfter(user, startOfDay);
    }

    /**
     * Get usage statistics for a user
     * 
     * @param user The user
     * @return UsageStats object
     */
    public UsageStats getUsageStats(User user) {
        return new UsageStats(
            getDailyConversionCount(user),
            getMonthlyConversionCount(user),
            YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        );
    }

    /**
     * Reset monthly usage for a specific month (admin function)
     */
    @Transactional
    public void resetMonthlyUsage(User user, String yearMonth) {
        monthlyUsageRepository.findByUserAndYearMonth(user, yearMonth).ifPresent(usage -> {
            usage.setConversionCount(0);
            monthlyUsageRepository.save(usage);
            log.info("Reset monthly usage for user: {} | Month: {}", user.getId(), yearMonth);
        });
    }

    // ===== Exception Class =====

    /**
     * Thrown when user exceeds usage limits
     */
    public static class UsageLimitExceededException extends RuntimeException {
        public UsageLimitExceededException(String message) {
            super(message);
        }
    }

    // ===== Usage Statistics DTO =====

    /**
     * Contains usage statistics for a user
     */
    public record UsageStats(
        long dailyCount,
        long monthlyCount,
        String currentMonth
    ) {}
}
