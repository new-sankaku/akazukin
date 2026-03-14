package com.akazukin.domain.model;

public record AiResponse(
    String generatedText,
    int tokensUsed,
    long durationMs,
    String modelName
) {
}
