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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiProvider implements AiProvider {
    
    private final WebClient webClient;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Override
    public Mono<String> generateResponse(String prompt, String plan) {
        String model = getModelForPlan(plan);
        
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
                    log.debug("Generated AI response length: {} characters", content.length());
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
     * Map user plan to GPT model
     */
    private String getModelForPlan(String plan) {
        if (plan == null) return "gpt-3.5-turbo";
        switch (plan.toUpperCase()) {
            case "STARTER":
                return "gpt-3.5-turbo";          // Efficient, fast
            case "PROFESSIONAL":
                return "gpt-4";                     // High quality
            case "ENTERPRISE":
                return "gpt-4-turbo";               // Best for complex tasks & high volume
            default:
                return "gpt-3.5-turbo";              // Fallback
        }
    }
}
