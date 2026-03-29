package com.converter.backend.service;

import com.converter.backend.dto.openAI.OpenAIRequest;
import com.converter.backend.dto.openAI.OpenAIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI implementation of AiProvider
 * 
 * Enhanced for plan-based model routing:
 * - FREE: GPT-5 nano (cheapest)
 * - PRO: GPT-5 mini (balanced)
 * - ENTERPRISE: GPT-5 (best quality)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiProvider implements AiProvider {
    
    private final WebClient webClient;
    private final TokenUsageLogger tokenUsageLogger;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * Original method - for backward compatibility
     */
    @Override
    public Mono<String> generateResponse(String prompt, String plan) {
        String model = getModelForPlan(plan);
        return generateResponse(prompt, model, plan);
    }

    /**
     * Enhanced method with explicit model
     */
    public Mono<String> generateResponse(String prompt, String model, String plan) {
        log.info("Generating AI response using model: {} for plan: {}", model, plan);
        
        OpenAIRequest request = new OpenAIRequest(
                model,
                List.of(Map.of("role", "user", "content", prompt))
        );
        
        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenAI API error: {}", errorBody);
                                    return Mono.error(new RuntimeException("OpenAI API error: " + errorBody));
                                })
                )
                .bodyToMono(OpenAIResponse.class)
                .timeout(Duration.ofSeconds(30))
                .map(response -> {
                    if (response == null || response.choices() == null || response.choices().isEmpty()) {
                        throw new RuntimeException("Empty or invalid response from OpenAI API");
                    }
                    String content = response.choices().get(0).message().get("content");
                    
                    // Log token usage
                    int inputTokens = estimateTokens(prompt);
                    int outputTokens = estimateTokens(content);
                    double estimatedCost = tokenUsageLogger.estimateCost(model, inputTokens, outputTokens);
                    log.debug("Generated AI response | Tokens: in={}, out={} | Cost: ${}", 
                        inputTokens, outputTokens, String.format("%.6f", estimatedCost));
                    
                    return content;
                })
                .doOnError(error -> log.error("Error generating AI response", error));
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - we could make a lightweight API call here
            return apiKey != null && !apiKey.trim().isEmpty() && apiUrl != null && !apiUrl.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Health check failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    /**
     * Map subscription plan to GPT-5 model tier
     * 
     * Key strategy:
     * - FREE: Cheapest model for cost efficiency
     * - PRO: Balanced model for quality/cost ratio
     * - ENTERPRISE: Best model for production quality
     */
    public String getModelForPlan(String plan) {
        if (plan == null) {
            return "gpt-5-nano";  // Default to cheapest for unknown plans
        }

        return switch (plan.toUpperCase()) {
            // ===== NEW PLAN NAMES =====
            case "FREE" -> "gpt-5-nano";              // Cheapest: ~$0.00003 per 1M tokens
            case "PRO" -> "gpt-5-mini";               // Balanced: ~$0.00015 per 1M tokens
            case "ENTERPRISE" -> "gpt-5";             // Best: ~$0.03 per 1M tokens

            // ===== LEGACY SUPPORT =====
            case "STARTER", "BASIC" -> "gpt-5-nano";
            case "PROFESSIONAL", "STANDARD" -> "gpt-5-mini";
            case "PREMIUM", "ULTIMATE" -> "gpt-5";

            default -> "gpt-5-nano";                  // Safe fallback
        };
    }

    /**
     * Get model tier name for logging
     */
    public String getModelTier(String model) {
        return switch (model.toLowerCase()) {
            case "gpt-5-nano" -> "ULTRA_CHEAP";
            case "gpt-5-mini" -> "CHEAP";
            case "gpt-5-standard" -> "STANDARD";
            case "gpt-5" -> "PREMIUM";
            default -> "UNKNOWN";
        };
    }

    /**
     * Estimate token count from text (4 chars ~ 1 token)
     * This is a simple approximation; use official tokenizer for accuracy
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        return Math.max(1, text.length() / 4);
    }

    /**
     * Cost estimate per 1M tokens (based on OpenAI GPT-5 pricing)
     * Update these values based on actual OpenAI pricing
     */
    public double getCostPer1mTokens(String model, boolean isOutput) {
        if (model == null) {
            return 0;
        }

        if (!isOutput) { // Input tokens
            return switch (model.toLowerCase()) {
                case "gpt-5-nano" -> 0.00003;        // Cheapest input
                case "gpt-5-mini" -> 0.00015;        // Standard input
                case "gpt-5-standard" -> 0.003;      // Higher quality
                case "gpt-5" -> 0.03;                // Premium quality
                default -> 0.00015;                  // Fallback
            };
        } else { // Output tokens (typically more expensive)
            return switch (model.toLowerCase()) {
                case "gpt-5-nano" -> 0.0001;         // ~3x input cost
                case "gpt-5-mini" -> 0.0006;         // ~4x input cost
                case "gpt-5-standard" -> 0.012;      // ~4x input cost
                case "gpt-5" -> 0.06;                // ~2x input cost
                default -> 0.0006;                   // Fallback
            };
        }
    }
}

