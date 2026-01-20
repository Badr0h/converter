package com.converter.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Facade service for AI operations
 * Uses the configured AI provider (OpenAI by default)
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class AiService {

    private final AiProvider aiProvider;

    /**
     * Generate AI response using the configured provider
     *
     * @param prompt The user prompt
     * @param plan User plan: BASIC, PRO, PREMIUM, BUSINESS
     * @return AI-generated response as Mono<String>
     */
    public Mono<String> generateResponse(String prompt, String plan) {
        log.info("Generating AI response for plan: {} using provider: {}", plan, aiProvider.getProviderName());
        
        if (!aiProvider.isHealthy()) {
            log.error("AI provider {} is not healthy", aiProvider.getProviderName());
            return Mono.error(new RuntimeException("AI service is currently unavailable"));
        }
        
        return aiProvider.generateResponse(prompt, plan)
                .doOnSuccess(response -> log.info("Successfully generated AI response of length: {}", response.length()))
                .doOnError(error -> log.error("Failed to generate AI response", error));
    }

    /**
     * Get the current provider name
     *
     * @return provider name
     */
    public String getCurrentProvider() {
        return aiProvider.getProviderName();
    }

    /**
     * Check if the AI service is healthy
     *
     * @return true if healthy
     */
    public boolean isHealthy() {
        return aiProvider.isHealthy();
    }
}
