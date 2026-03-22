package com.akazukin.application.dto;

import java.util.UUID;

public record AiCompareColumnDto(
    UUID personaId,
    String personaName,
    String tone,
    String language,
    String generatedText,
    int tokensUsed,
    long durationMs,
    String modelName
) {}
