package com.converter.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.converter.backend.dto.conversion.ConversionCreateDto;
import com.converter.backend.dto.conversion.ConversionResponseDto;
import com.converter.backend.exception.IllegalStateException;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.Conversion;
import com.converter.backend.model.User;
import com.converter.backend.model.Plan;
import com.converter.backend.repository.ConversionRepository;
import com.converter.backend.repository.UserRepository;
import com.converter.backend.model.Subscription;
import com.converter.backend.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * ConversionService - Refactored for multi-plan SaaS architecture
 * 
 * Enhanced with:
 * - Plan-based prompt generation (PromptBuilder)
 * - Usage limiting (daily for FREE, monthly for PRO/ENTERPRISE)
 * - Intelligent caching (deduplication of identical requests)
 * - Token usage logging (cost tracking)
 * - Dynamic model routing (cheap for FREE, best for ENTERPRISE)
 * 
 * Key flow:
 * 1. Validate user has active subscription
 * 2. Get plan configuration
 * 3. Check usage limits
 * 4. Check cache for identical request
 * 5. Build plan-optimized prompt
 * 6. Call AI with plan-specific model
 * 7. Log token usage and cost
 * 8. Record conversion and usage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ConversionRepository conversionRepository;
    private final AiService aiService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    // ===== NEW SERVICES =====
    private final PromptBuilder promptBuilder;
    private final UsageLimiter usageLimiter;
    private final ConversionCache conversionCache;
    private final TokenUsageLogger tokenUsageLogger;
    private final OpenAiProvider openAiProvider;

    public Page<ConversionResponseDto> findByUserIdPaginated(Long userId, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findByUserId(userId, pageable)
                .map(this::mapToConversionResponseDto);
    }

    public List<ConversionResponseDto> findByUserId(Long userId){
        // Limite à 100 résultats les plus récents pour éviter les surcharges
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findByUserId(userId, pageable)
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();  
    }

    public List<ConversionResponseDto> getAllConversions(){
        return conversionRepository.findAll()
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();  
    }

    public List<ConversionResponseDto> getRecentActivity(int limit) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return conversionRepository.findAll(pageable)
                .stream()
                .map(this::mapToConversionResponseDto)
                .toList();
    }

    public void deleteConversionById(Long id) {
        if (!conversionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversion not found with id: " + id);
        }
        conversionRepository.deleteById(id);
    }

    public ConversionResponseDto getConversionById(Long id){
        Conversion conversion = conversionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("conversion not found with id : " + id));

        return mapToConversionResponseDto(conversion);
    }

    /**
     * Create a new conversion with full plan-based optimization
     * 
     * STEP 1: Validate subscription
     * STEP 2: Check usage limits
     * STEP 3: Check cache
     * STEP 4: Build optimized prompt
     * STEP 5: Call AI with plan-specific model
     * STEP 6: Record usage and log costs
     */
    @Transactional
    public ConversionResponseDto createConversion(ConversionCreateDto dto, Long userId) {
        log.info("Creating conversion for user: {}", userId);
        
        // STEP 1: Get user and validate subscription
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var subscriptionList = subscriptionRepository.findByUserAndStatusList(userId, Subscription.Status.ACTIVE.name());
        Subscription subscription = subscriptionList.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active subscription required. Please upgrade."));

        Plan plan = subscription.getPlan();
        if (plan == null) {
            throw new IllegalStateException("Subscription must be associated with a plan");
        }

        String planName = plan.getName().toUpperCase();
        log.debug("User {} has plan: {}", userId, planName);

        // ===== STEP 2: Check usage limits =====
        // Throws LimitExceededException if limit exceeded (handled by GlobalExceptionHandler)
        usageLimiter.checkUsageLimit(
            user,
            planName,
            plan.getDailyLimit(),
            plan.getMonthlyLimit()
        );
        log.debug("Usage limit check passed for user: {}", userId);

        // ===== STEP 3: Try to get cached result =====
        Optional<Conversion> cachedConversion = conversionCache.getCachedExpression(
            user,
            dto.getInputFormat(),
            dto.getOutputFormat(),
            dto.getPrompt(),
            plan.getCacheTtlHours()
        );

        if (cachedConversion.isPresent()) {
            log.info("Cache HIT! Using existing conversion {} for user {}", cachedConversion.get().getId(), userId);
            // Log cache hit without making AI call
            int promptTokens = estimateTokens(dto.getPrompt());
            tokenUsageLogger.logTokenUsage(userId, planName, promptTokens, 0, true, 0.0);
            
            return mapToConversionResponseDto(cachedConversion.get());
        }

        log.debug("Cache MISS for user {}, will call AI", userId);

        // ===== STEP 4: Build plan-optimized prompt =====
        String optimizedPrompt = promptBuilder.buildPrompt(
            planName,
            dto.getInputFormat().name(),
            dto.getOutputFormat().name(),
            dto.getPrompt()
        );
        
        int promptTokens = estimateTokens(optimizedPrompt);
        log.debug("Optimized prompt tokens: {} for plan: {}", promptTokens, planName);

        // ===== STEP 5: Get plan-specific model and call AI =====
        String aiModel = plan.getAiModel();
        if (aiModel == null || aiModel.isEmpty()) {
            aiModel = openAiProvider.getModelForPlan(planName);
        }
        
        log.info("Calling AI | Model: {} | Plan: {} | User: {}", aiModel, planName, userId);

        String aiResponse;
        try {
            aiResponse = aiService.generateResponseSync(optimizedPrompt, planName);
        } catch (Exception e) {
            log.error("Failed to generate AI response for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to generate AI response", e);
        }

        // ===== STEP 6: Log token usage and costs =====
        int responseTokens = estimateTokens(aiResponse);
        double estimatedCost = tokenUsageLogger.estimateCost(aiModel, promptTokens, responseTokens);
        
        tokenUsageLogger.logTokenUsage(
            userId,
            planName,
            promptTokens,
            responseTokens,
            false,  // Not from cache
            estimatedCost
        );

        // ===== Save conversion =====
        Conversion conversion = new Conversion();
        conversion.setInputFormat(dto.getInputFormat());
        conversion.setOutputFormat(dto.getOutputFormat());
        conversion.setPrompt(dto.getPrompt());  // Store original, not optimized
        conversion.setAiResponse(aiResponse);
        conversion.setUser(user);

        Conversion savedConversion = conversionRepository.save(conversion);
        log.debug("Saved conversion: {}", savedConversion.getId());

        // ===== Record usage =====
        usageLimiter.recordConversion(user);
        log.info("Recorded conversion usage for user: {}", userId);

        return mapToConversionResponseDto(savedConversion);
    }

    @Transactional(readOnly = true)
    public long getCurrentUserMonthlyConversionCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByEmail(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUsername));

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        
        return conversionRepository.countByUserIdAndCreatedAtAfter(user.getId(), startOfMonth);
    }

    /**
     * Get usage statistics for current user
     */
    public UsageStatsDto getUserUsageStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UsageLimiter.UsageStats stats = usageLimiter.getUsageStats(user);
        
        // Get user's plan limits
        var subscriptionList = subscriptionRepository.findByUserAndStatusList(userId, Subscription.Status.ACTIVE.name());
        Subscription subscription = subscriptionList.stream().findFirst().orElse(null);
        
        return new UsageStatsDto(
            stats.dailyCount(),
            stats.monthlyCount(),
            subscription != null && subscription.getPlan() != null ? subscription.getPlan().getDailyLimit() : 0,
            subscription != null && subscription.getPlan() != null ? subscription.getPlan().getMonthlyLimit() : 0,
            stats.currentMonth()
        );
    }

    /**
     * Estimate token count (used for logging and cost calculation)
     * Simple approximation: 4 characters ≈ 1 token
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        return Math.max(1, text.length() / 4);
    }

    public ConversionResponseDto mapToConversionResponseDto(Conversion conversion){
        ConversionResponseDto dto = new ConversionResponseDto();
        dto.setId(conversion.getId());
        dto.setUserId(conversion.getUser() != null ? conversion.getUser().getId() : null);
        dto.setUserEmail(conversion.getUser() != null ? conversion.getUser().getEmail() : null);
        dto.setInputFormat(conversion.getInputFormat());
        dto.setOutputFormat(conversion.getOutputFormat());
        dto.setAiResponse(conversion.getAiResponse());
        dto.setPrompt(conversion.getPrompt());
        dto.setCreatedAt(conversion.getCreatedAt());
        return dto;
    }

    // ===== DTOs =====

    /**
     * Usage statistics for user
     */
    public record UsageStatsDto(
        long dailyUsage,
        long monthlyUsage,
        int dailyLimit,
        int monthlyLimit,
        String currentMonth
    ) {
        public double getDailyPercentage() {
            return dailyLimit > 0 ? (double) dailyUsage / dailyLimit * 100 : 0;
        }

        public double getMonthlyPercentage() {
            return monthlyLimit > 0 ? (double) monthlyUsage / monthlyLimit * 100 : 0;
        }

        public boolean isDailyLimitExceeded() {
            return dailyLimit > 0 && dailyUsage >= dailyLimit;
        }

        public boolean isMonthlyLimitExceeded() {
            return monthlyLimit > 0 && monthlyUsage >= monthlyLimit;
        }
    }
}

