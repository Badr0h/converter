package com.converter.backend.dto.openAI;
import java.util.List;
import java.util.Map;

public record OpenAIResponse(
        List<Choice> choices
) {
    public record Choice(
            Map<String, String> message
    ) {}
}