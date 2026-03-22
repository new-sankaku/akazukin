package com.akazukin.application.dto;

import java.util.UUID;

public record ABTestVariantGenerateRequestDto(
    String originalText,
    UUID personaId
) {}
