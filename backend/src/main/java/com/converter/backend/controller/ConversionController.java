package com.converter.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.backend.service.ConversionService;
import com.converter.backend.model.User;
import com.converter.backend.service.AuthService;

/**
 * Conversion Controller
 * 
 * Handles conversion requests with plan-based optimization:
 * - Validates subscription before conversion
 * - Enforces usage limits (daily for FREE, monthly for PRO/ENTERPRISE)
 * - Applies intelligent caching
 * - Logs token usage and costs
 * - Routes to plan-specific AI models
 */
@Slf4j
@RestController
@RequestMapping("/api/conversions")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final AuthService authService;

    /**
     * Get all conversions for the current user (paginated)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversionResponseDto>> getUserConversions(){
        User currentUser = authService.getCurrentUser();
        log.debug("Fetching conversions for user: {}", currentUser.getId());
        return ResponseEntity.ok(conversionService.findByUserId(currentUser.getId()));
    }
    
    /**
     * Get a specific conversion by ID
     * Verifies user ownership
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversionResponseDto> getConversionById(@PathVariable Long id){
        User currentUser = authService.getCurrentUser();
        log.debug("Fetching conversion: {} for user: {}", id, currentUser.getId());

        ConversionResponseDto conversion = conversionService.getConversionById(id);
        
        // Verify user ownership
        if (!conversion.getUserId().equals(currentUser.getId())) {
            log.warn("Access denied: User {} attempted to access conversion {}", currentUser.getId(), id);
            throw new SecurityException("Access denied: You can only access your own conversions");
        }
        
        return ResponseEntity.ok(conversion);
    }

    /**
     * Create a new conversion (main endpoint)
     * 
     * Flow:
     * 1. Validate subscription
     * 2. Check usage limits
     * 3. Check cache
     * 4. Generate optimized prompt for plan
     * 5. Call AI with plan-specific model
     * 6. Log token usage and costs
     * 7. Return result with usage information
     * 
     * @param conversion Conversion request DTO
     * @return Created conversion response with AI result
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversionResponseDto> createConversion(
            @Valid @RequestBody ConversionCreateDto conversion) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating conversion for user: {} | Input: {} -> Output: {}", 
            currentUser.getId(), 
            conversion.getInputFormat(), 
            conversion.getOutputFormat()
        );

        try {
            ConversionResponseDto createdConversion = conversionService.createConversion(
                conversion, 
                currentUser.getId()
            );
            
            log.info("Conversion created successfully | ID: {} | User: {}", 
                createdConversion.getId(), 
                currentUser.getId()
            );

            return ResponseEntity
                .created(URI.create("/api/conversions/" + createdConversion.getId()))
                .body(createdConversion);
        } catch (Exception e) {
            log.error("Error creating conversion for user {}: {}", currentUser.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Get usage statistics for current user
     * Shows daily/monthly usage and limits
     * 
     * Response includes:
     * - dailyUsage: Number of conversions today (FREE plan only)
     * - monthlyUsage: Number of conversions this month (PRO & ENTERPRISE)
     * - dailyLimit: Daily limit for FREE plan
     * - monthlyLimit: Monthly limit for PRO/ENTERPRISE
     * - dailyPercentage: % of daily limit used
     * - monthlyPercentage: % of monthly limit used
     * - isDailyLimitExceeded: Boolean flag
     * - isMonthlyLimitExceeded: Boolean flag
     */
    @GetMapping("/stats/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversionService.UsageStatsDto> getUsageStats(){
        User currentUser = authService.getCurrentUser();
        log.debug("Fetching usage stats for user: {}", currentUser.getId());
        
        ConversionService.UsageStatsDto stats = conversionService.getUserUsageStats(currentUser.getId());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint for conversions service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "conversion-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}

