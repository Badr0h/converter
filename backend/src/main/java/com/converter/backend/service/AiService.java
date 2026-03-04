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
     * @param plan User plan: STARTER, PROFESSIONAL, ENTERPRISE
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
     * Generate AI response synchronously (blocking but with timeout)
     * Use this method when you need to work with traditional Spring MVC controllers
     *
     * @param prompt The user prompt
     * @param plan User plan: STARTER, PROFESSIONAL, ENTERPRISE
     * @return AI-generated response as String
     * @throws RuntimeException if generation fails or times out
     */
    public String generateResponseSync(String prompt, String plan) {
        log.info("Generating AI response synchronously for plan: {} using provider: {}", plan, aiProvider.getProviderName());
        
        if (!aiProvider.isHealthy()) {
            log.error("AI provider {} is not healthy", aiProvider.getProviderName());
            throw new RuntimeException("AI service is currently unavailable");
        }
        
        try {
            return generateResponse(prompt, plan)
                    .timeout(java.time.Duration.ofSeconds(30)) // Timeout de 30 secondes
                    .block(); // Block uniquement dans cette méthode isolée
        } catch (Exception e) {
            log.error("Failed to generate AI response synchronously", e);
            throw new RuntimeException("Failed to generate AI response: " + e.getMessage(), e);
        }
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
