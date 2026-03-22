package com.akazukin.application.dto;

import java.util.UUID;

public record TemplateMatchItemDto(
    int rank,
    UUID templateId,
    String templateName,
    String reason,
    int score
) {}
