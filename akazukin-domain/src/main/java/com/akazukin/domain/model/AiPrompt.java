package com.akazukin.domain.model;

public record AiPrompt(
    String systemPrompt,
    String userPrompt,
    double temperature,
    int maxTokens
) {

    public AiPrompt {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt must not be blank");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be greater than 0");
        }
    }
}
