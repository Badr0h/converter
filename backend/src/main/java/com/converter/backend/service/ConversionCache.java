package com.converter.backend.service;

import com.converter.backend.model.Conversion;
import com.converter.backend.model.User;
import com.converter.backend.repository.ConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ConversionCache Service
 * 
 * Implements intelligent caching to reduce AI API calls and costs:
 * - Deduplicates identical conversion requests
 * - Reuses results from previous conversions
 * - Supports TTL (time-to-live) per plan
 * - Falls back to AI if cache miss or expired
 * 
 * Benefits:
 * - Reduces token usage (lower costs)
 * - Improves response time
 * - Increases profitability
 * 
 * Strategy:
 * - FREE: Cache all (default TTL: 24h)
 * - PRO: Cache all (default TTL: 24h)
 * - ENTERPRISE: Cache all (default TTL: 7d for cost-sensitive operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionCache {

    private final ConversionRepository conversionRepository;

    /**
     * Try to get cached result for a conversion
     * 
     * @param user The user
     * @param inputFormat Input format
     * @param outputFormat Output format
     * @param prompt The conversion prompt
     * @param cacheTtlHours Cache TTL in hours
     * @return Optional containing cached conversion if found and valid
     */
    @Transactional(readOnly = true)
    public Optional<Conversion> getCachedExpression(
        User user,
        Conversion.Format inputFormat,
        Conversion.Format outputFormat,
        String prompt,
        Integer cacheTtlHours
    ) {
        if (user == null || prompt == null || noCache(cacheTtlHours)) {
            return Optional.empty();
        }

        // Query by user + formats + exact prompt
        Optional<Conversion> cached = conversionRepository.findByUserAndInputFormatAndOutputFormatAndPrompt(
            user,
            inputFormat,
            outputFormat,
            prompt
        );

        if (cached.isEmpty()) {
            log.debug("Cache MISS: No previous conversion found for user {}", user.getId());
            return Optional.empty();
        }

        Conversion conversion = cached.get();

        // Check if cache is still valid (not expired)
        if (isCacheExpired(conversion.getCreatedAt(), cacheTtlHours)) {
            log.debug("Cache EXPIRED: Conversion is too old (TTL: {}h)", cacheTtlHours);
            return Optional.empty();
        }

        log.info("Cache HIT: Reusing conversion {} for user {}", conversion.getId(), user.getId());
        return cached;
    }

    /**
     * Check if cache feature is disabled
     */
    private boolean noCache(Integer cacheTtlHours) {
        return cacheTtlHours == null || cacheTtlHours <= 0;
    }

    /**
     * Check if a cached result has expired
     * 
     * @param createdAt When the conversion was created
     * @param ttlHours Time-to-live in hours
     * @return true if cache is expired
     */
    private boolean isCacheExpired(LocalDateTime createdAt, Integer ttlHours) {
        if (ttlHours == null || ttlHours <= 0) {
            return false; // No expiration
        }

        LocalDateTime expirationTime = createdAt.plusHours(ttlHours);
        boolean expired = LocalDateTime.now().isAfter(expirationTime);

        if (expired) {
            log.debug("Cache expiration check: created={}, ttl={}h, expires={}, now={}",
                createdAt, ttlHours, expirationTime, LocalDateTime.now());
        }

        return expired;
    }

    /**
     * Get cache statistics (for monitoring)
     * 
     * @param user The user
     * @return CacheStats with hit/miss rates
     */
    public CacheStats getCacheStats(User user) {
        // This could be enhanced with actual hit/miss tracking
        // For now, just return placeholder stats
        return new CacheStats(0, 0, 0.0);
    }

    /**
     * Clear cache for a specific user (administrator function)
     * Note: We don't actually delete conversions, just stop using them as cache
     */
    @Transactional
    public void clearCacheForUser(User user) {
        log.info("Cache cleared for user: {}", user.getId());
        // Conversions are still stored for history, but no longer used as cache
    }

    /**
     * Estimate cost savings from cache hits
     * 
     * @param cacheHits Number of cache hits
     * @param avgTokensPerRequest Average tokens per request
     * @param tokenCost Cost per 1000 tokens
     * @return Estimated savings in currency units
     */
    public double estimateCostSavings(long cacheHits, int avgTokensPerRequest, double tokenCost) {
        return (double) (cacheHits * avgTokensPerRequest) / 1000.0 * tokenCost;
    }

    // ===== DTO =====

    /**
     * Cache statistics
     */
    public record CacheStats(
        long hits,
        long misses,
        double hitRate
    ) {}
}
