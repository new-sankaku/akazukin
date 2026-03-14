package com.akazukin.application.dto;

import java.util.UUID;

public record AiGenerateRequestDto(
    UUID personaId,
    String prompt,
    double temperature,
    int maxTokens
) {}
