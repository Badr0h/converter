package com.converter.backend.dto.openAI;
import java.util.List;
import java.util.Map;

public record OpenAIRequest(
        String model,
        List<Map<String, String>> messages
) {}