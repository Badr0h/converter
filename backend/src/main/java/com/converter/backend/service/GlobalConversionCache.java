package com.converter.backend.service;

import com.converter.backend.model.Conversion;
import com.converter.backend.repository.ConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * GlobalConversionCache Service
 * 
 * Implements shared caching across all users:
 * - Cache key = (inputFormat + outputFormat + prompt hash)
 * - Any user gets cache hit for same conversion
 * - Reduces AI API calls dramatically
 * - Improves system profitability
 * 
 * Features:
 * - Deduplicates identical requests globally
 * - Configurable TTL by plan
 * - Fallback to AI on cache miss
 * - Tracks cache hits for analytics
 * 
 * Example:
 * - User A converts "2+2" to "PYTHON" → calls AI
 * - User B converts "2+2" to "PYTHON" → gets cached result instantly
 * - Cost saved: 100% (one API call instead of two)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalConversionCache {

    private final ConversionRepository conversionRepository;

    /**
     * Try to get globally cached result for a conversion
     * 
     * @param inputFormat Input format
     * @param outputFormat Output format
     * @param prompt The conversion prompt (search key)
     * @param cacheTtlHours Cache TTL in hours (plan-specific)
     * @return Optional containing cached conversion if found and valid
     */
    @Transactional(readOnly = true)
    public Optional<Conversion> getGlobalCache(
        Conversion.Format inputFormat,
        Conversion.Format outputFormat,
        String prompt,
        Integer cacheTtlHours
    ) {
        if (prompt == null || noCaching(cacheTtlHours)) {
            return Optional.empty();
        }

        try {
            // Query any conversion with matching formats and prompt (ignores user)
            Optional<Conversion> cached = conversionRepository
                .findFirstByInputFormatAndOutputFormatAndPromptOrderByCreatedAtDesc(
                    inputFormat,
                    outputFormat,
                    prompt
                );

            if (cached.isEmpty()) {
                log.debug("Global cache MISS: No conversion found for {} → {}", inputFormat, outputFormat);
                return Optional.empty();
            }

            Conversion conversion = cached.get();

            // Check if cache is still valid (not expired)
            if (isCacheExpired(conversion.getCreatedAt(), cacheTtlHours)) {
                log.debug("Global cache EXPIRED: Conversion is too old (TTL: {}h)", cacheTtlHours);
                return Optional.empty();
            }

            log.info("Global cache HIT: Reusing result for {} → {} (saved 1 API call)", inputFormat, outputFormat);
            return Optional.of(conversion);

        } catch (Exception e) {
            log.warn("Error checking global cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if cache is expired based on TTL
     */
    private boolean isCacheExpired(LocalDateTime createdAt, Integer cacheTtlHours) {
        if (cacheTtlHours == null || cacheTtlHours <= 0) {
            return false; // Never expires
        }
        LocalDateTime expiryTime = createdAt.plusHours(cacheTtlHours);
        boolean expired = LocalDateTime.now().isAfter(expiryTime);
        if (expired) {
            log.debug("Cache expired: created at {}, TTL: {}h", createdAt, cacheTtlHours);
        }
        return expired;
    }

    /**
     * Check if caching is disabled
     */
    private boolean noCaching(Integer cacheTtlHours) {
        return cacheTtlHours == null || cacheTtlHours == 0;
    }
}
