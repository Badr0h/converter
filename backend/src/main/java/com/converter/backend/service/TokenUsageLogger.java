package com.converter.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TokenUsageLogger Service
 * 
 * Tracks and logs token usage for cost monitoring and optimization:
 * - Logs tokens used per request
 * - Tracks costs for each plan
 * - Generates analytics for profitability
 * - Helps identify cost optimization opportunities
 * 
 * Features:
 * - Per-request token logging
 * - Aggregate cost tracking
 * - ROI calculations
 * - Plan performance metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageLogger {

    /**
     * Log token usage for a conversion request
     * 
     * @param userId User ID
     * @param planName Plan name ("FREE", "PRO", "ENTERPRISE")
     * @param inputTokens Tokens in the prompt
     * @param outputTokens Tokens in the response
     * @param usesCacheHit Whether this used cached result
     * @param costInUsd Estimated cost in USD
     */
    public void logTokenUsage(Long userId, String planName, int inputTokens, int outputTokens, 
                              boolean usesCacheHit, double costInUsd) {
        int totalTokens = inputTokens + outputTokens;
        
        String cacheStatus = usesCacheHit ? "CACHED" : "GENERATED";
        
        log.info("TOKEN_USAGE | User: {} | Plan: {} | Input: {} | Output: {} | Total: {} | Status: {} | Cost: ${:.6f}",
            userId,
            planName,
            inputTokens,
            outputTokens,
            totalTokens,
            cacheStatus,
            costInUsd
        );

        // Additional structured logging for analytics
        log.debug("Detailed token usage: user={}, plan={}, input_tokens={}, output_tokens={}, cache_hit={}, cost_usd={}",
            userId, planName, inputTokens, outputTokens, usesCacheHit, costInUsd);
    }

    /**
     * Estimate cost based on model and tokens used
     * 
     * @param aiModel The AI model used ("gpt-5-nano", "gpt-5-mini", "gpt-5")
     * @param inputTokens Input tokens
     * @param outputTokens Output tokens
     * @return Estimated cost in USD
     */
    public double estimateCost(String aiModel, int inputTokens, int outputTokens) {
        if (aiModel == null) {
            return 0.0;
        }

        // Pricing per 1M tokens (example rates - update with actual OpenAI pricing)
        double inputCost = switch (aiModel.toLowerCase()) {
            case "gpt-5-nano" -> 0.00003;         // Cheapest
            case "gpt-5-mini" -> 0.00015;         // Medium
            case "gpt-5" -> 0.03;                 // Most expensive
            default -> 0.00015; // Default fallback
        };

        double outputCostMultiplier = switch (aiModel.toLowerCase()) {
            case "gpt-5-nano" -> 0.0001;          // 3x input
            case "gpt-5-mini" -> 0.0006;          // 4x input
            case "gpt-5" -> 0.06;                 // 2x input
            default -> 0.0006;
        };

        double inputCostUsd = (inputTokens / 1_000_000.0) * inputCost;
        double outputCostUsd = (outputTokens / 1_000_000.0) * (inputCost * outputCostMultiplier);

        return inputCostUsd + outputCostUsd;
    }

    /**
     * Log monthly cost summary (called by scheduler)
     * 
     * @param yearMonth Format: "YYYY-MM"
     * @param totalTokens Total tokens used in the month
     * @param totalCostUsd Total cost in USD
     * @param cacheHitCount Count of cache hits
     */
    public void logMonthlySummary(String yearMonth, long totalTokens, double totalCostUsd, long cacheHitCount) {
        double costSavingsFromCache = totalTokens > 0 ? (cacheHitCount * 100.0 / totalTokens) : 0;
        
        log.info("MONTHLY_SUMMARY | Month: {} | Total Tokens: {} | Total Cost: ${:.2f} | Cache Hits: {} | Cache Hit Rate: {:.2f}%",
            yearMonth,
            totalTokens,
            totalCostUsd,
            cacheHitCount,
            costSavingsFromCache
        );
    }

    /**
     * Log profitability metrics (revenue vs cost)
     * 
     * @param planName Plan name
     * @param monthlyRevenue Monthly subscription revenue
     * @param monthlyAiCost Estimated AI API costs
     * @param conversionsCount Number of conversions
     */
    public void logProfitabilityMetrics(String planName, double monthlyRevenue, double monthlyAiCost, long conversionsCount) {
        double profit = monthlyRevenue - monthlyAiCost;
        double margin = monthlyRevenue > 0 ? (profit / monthlyRevenue * 100) : 0;
        double costPerConversion = conversionsCount > 0 ? (monthlyAiCost / conversionsCount) : 0;
        
        log.info("PROFITABILITY | Plan: {} | Revenue: ${:.2f} | AI Cost: ${:.2f} | Profit: ${:.2f} | Margin: {:.2f}% | Cost/Conversion: ${:.4f}",
            planName,
            monthlyRevenue,
            monthlyAiCost,
            profit,
            margin,
            costPerConversion
        );
    }

    /**
     * Log optimization recommendations
     * 
     * @param recommendation Optimization recommendation
     */
    public void logOptimizationTip(String recommendation) {
        log.warn("OPTIMIZATION_TIP: {}", recommendation);
    }

    /**
     * Model cost tier enum
     */
    public enum CostTier {
        ULTRA_CHEAP(0.00003, "gpt-5-nano"),
        CHEAP(0.00015, "gpt-5-mini"),
        STANDARD(0.003, "gpt-5-standard"),
        EXPENSIVE(0.03, "gpt-5");

        private final double costPer1mTokens;
        private final String modelName;

        CostTier(double costPer1mTokens, String modelName) {
            this.costPer1mTokens = costPer1mTokens;
            this.modelName = modelName;
        }

        public double getCostPer1mTokens() {
            return costPer1mTokens;
        }

        public String getModelName() {
            return modelName;
        }
    }

    /**
     * Token usage summary
     */
    public record TokenUsageSummary(
        long totalTokens,
        int requestCount,
        long cacheHits,
        double totalCost,
        String period
    ) {
        public double getAvgTokensPerRequest() {
            return requestCount > 0 ? (double) totalTokens / requestCount : 0;
        }

        public double getAvgCostPerRequest() {
            return requestCount > 0 ? totalCost / requestCount : 0;
        }

        public double getCacheHitRate() {
            long totalRequests = requestCount;
            return totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        }
    }
}
