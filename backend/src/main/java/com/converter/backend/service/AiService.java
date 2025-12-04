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

    public Mono<String> generateResponse(String prompt) {

        OpenAIRequest request = new OpenAIRequest(
                "gpt-3.5-turbo",
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
}
