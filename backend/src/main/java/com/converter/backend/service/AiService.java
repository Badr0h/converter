package com.converter.backend.service;

import com.converter.backend.dto.openAI.OpenAIRequest;
import com.converter.backend.dto.openAI.OpenAIResponse;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final WebClient webClient;

    public AiService(@Value("${openai.api.url}") String apiUrl,
                     @Value("${openai.api.key}") String apiKey) {

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Generate AI response using OpenAI GPT based on user plan
     *
     * @param prompt The user prompt
     * @param plan   User plan: BASIC, PRO, PREMIUM, BUSINESS
     * @return AI-generated response as Mono<String>
     */
    public Mono<String> generateResponse(String prompt, String plan) {
        String model = getModelForPlan(plan);

        OpenAIRequest request = new OpenAIRequest(
                model,
                List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Erreur API OpenAI: " + errorBody)))
                )
                .bodyToMono(OpenAIResponse.class)
                .map(response -> {
                    if (response == null || response.choices() == null || response.choices().isEmpty()) {
                        throw new RuntimeException("Réponse vide ou malformée retournée par l'API");
                    }
                    return response.choices().get(0).message().get("content");
                });
    }

    /**
     * Map user plan to GPT model
     */
    private String getModelForPlan(String plan) {
        switch (plan.toUpperCase()) {
            case "BASIC":
                return "gpt-5-mini";          // cheapest, fast
            case "PRO":
                return "gpt-5.2-instant";     // higher quality
            case "PREMIUM":
                return "gpt-5.2-thinking";    // best for complex tasks
            case "BUSINESS":
                return "gpt-5.2-pro";         // enterprise, heavy usage
            default:
                return "gpt-5-mini";          // fallback
        }
    }
}
