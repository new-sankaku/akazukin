package com.akazukin.application.dto;

public record AiGenerateResponseDto(
    String generatedText,
    int tokensUsed,
    long durationMs,
    String modelName
) {}
