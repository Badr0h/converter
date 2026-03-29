package com.converter.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PromptBuilder Service
 * 
 * Generates plan-specific prompts for AI conversion requests.
 * Each plan has optimized prompts for better quality/cost ratio.
 * 
 * Key features:
 * - Plan-based prompt templates (FREE, PRO, ENTERPRISE)
 * - Cost-optimized prompts (minimal tokens for Free, detailed for Pro/Enterprise)
 * - Placeholder substitution: {inputFormat}, {outputFormat}, {userInput}
 * - Returns ONLY code (no explanations or markdown)
 */
@Slf4j
@Service
public class PromptBuilder {

    /**
     * Build a plan-specific prompt for conversion
     * 
     * @param planName The subscription plan: "FREE", "PRO", "ENTERPRISE"
     * @param inputFormat Format of input (e.g., "PYTHON", "LATEX")
     * @param outputFormat Format of output (e.g., "JAVASCRIPT", "MATHML")
     * @param userInput The user's code/formula to convert
     * @return Optimized prompt for the plan
     */
    public String buildPrompt(String planName, String inputFormat, String outputFormat, String userInput) {
        log.debug("Building prompt for plan: {} | Input: {} -> Output: {}", planName, inputFormat, outputFormat);
        
        if (planName == null) {
            return buildFreePrompt(inputFormat, outputFormat, userInput);
        }
        
        return switch (planName.toUpperCase()) {
            case "FREE" -> buildFreePrompt(inputFormat, outputFormat, userInput);
            case "PRO" -> buildProPrompt(inputFormat, outputFormat, userInput);
            case "ENTERPRISE" -> buildEnterprisePrompt(inputFormat, outputFormat, userInput);
            default -> buildFreePrompt(inputFormat, outputFormat, userInput);
        };
    }

    /**
     * FREE PLAN PROMPT
     * 
     * Minimal tokens, simple instructions
     * Cost-optimized: ~50-100 tokens max
     * Model: GPT-5 nano (cheapest)
     */
    private String buildFreePrompt(String inputFormat, String outputFormat, String userInput) {
        return "Convert " + inputFormat + " to " + outputFormat + ":\n" + userInput + "\nReturn ONLY code, no explanation.";
    }

    /**
     * PRO PLAN PROMPT
     * 
     * Balanced approach, professional formatting
     * Quality-focused: ~150-300 tokens
     * Model: GPT-5 mini (balanced)
     */
    private String buildProPrompt(String inputFormat, String outputFormat, String userInput) {
        return """
Convert {inputFormat} to {outputFormat} with the following requirements:

1. Convert the provided code/formula from {inputFormat} to {outputFormat}
2. Maintain exact functionality and logic
3. Use clean, professional code style
4. Follow {outputFormat} best practices and conventions
5. Add meaningful variable names where applicable
6. Return ONLY the converted code, no explanations or markdown

Input format: {inputFormat}
Output format: {outputFormat}

Code to convert:
{userInput}

Return ONLY the converted code:
""".replace("{inputFormat}", inputFormat)
                .replace("{outputFormat}", outputFormat)
                .replace("{userInput}", userInput);
    }

    /**
     * ENTERPRISE PLAN PROMPT
     * 
     * Comprehensive, optimized for production
     * Full-featured: ~300-500 tokens
     * Model: GPT-5 (best available)
     */
    private String buildEnterprisePrompt(String inputFormat, String outputFormat, String userInput) {
        return """
You are an expert code converter. Convert {inputFormat} to {outputFormat} with precise quality control:

## Conversion Requirements
1. **Functional Equivalence**: Ensure 100% functional parity between input and output
2. **Code Style**: Follow {outputFormat} conventions and best practices
3. **Performance**: Optimize for readability and efficiency
4. **Error Handling**: Add appropriate error handling where missing
5. **Documentation**: Add concise inline comments for complex logic
6. **Type Safety**: Use proper type declarations for {outputFormat}
7. **Standards**: Follow SOLID principles and design patterns

## Input Format Details
- Format: {inputFormat}
- Ensure all input syntax is valid and complete

## Output Requirements
- Format: {outputFormat}
- Return ONLY the converted code (no markdown, no explanations)
- Include necessary imports/dependencies
- Preserve all original functionality
- Optimize variable naming for clarity

## Code to Convert
{userInput}

## Return Format
Provide ONLY the converted code, ready to run. Do not include:
- Explanations or comments about the conversion
- Markdown code blocks or formatting
- Alternative solutions or variations
- Theory or educational content

Converted code:
""".replace("{inputFormat}", inputFormat)
                .replace("{outputFormat}", outputFormat)
                .replace("{userInput}", userInput);
    }

    /**
     * Get prompt statistics for logging/analytics
     * 
     * @param prompt The generated prompt
     * @return Statistics map
     */
    public int estimateTokenCount(String prompt) {
        // Simple estimation: ~4 chars = 1 token (OpenAI average)
        if (prompt == null) return 0;
        return Math.max(1, prompt.length() / 4);
    }

    /**
     * Validate that prompt is safe and complete
     */
    public boolean isValidPrompt(String prompt) {
        return prompt != null && !prompt.trim().isEmpty() && prompt.length() < 10000;
    }
}
