package com.akazukin.application.dto;

import java.util.UUID;

public record AiTryoutResponseDto(
    UUID personaId,
    String originalText,
    String transformedText,
    int tokensUsed,
    long durationMs
) {}
