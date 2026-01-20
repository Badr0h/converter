package com.converter.backend.service;

import reactor.core.publisher.Mono;

/**
 * Interface for AI service providers
 * Allows for multiple AI implementations (OpenAI, Claude, etc.)
 */
public interface AiProvider {
    
    /**
     * Generate AI response based on user prompt and plan
     * 
     * @param prompt The user's mathematical formula prompt
     * @param plan The user's subscription plan (determines model quality)
     * @return AI-generated response as Mono<String>
     */
    Mono<String> generateResponse(String prompt, String plan);
    
    /**
     * Check if the provider is available and healthy
     * 
     * @return true if provider is available
     */
    boolean isHealthy();
    
    /**
     * Get the provider name for logging/monitoring
     * 
     * @return provider name
     */
    String getProviderName();
}
