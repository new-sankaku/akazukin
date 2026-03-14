package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AiPersonaDto(
    UUID id,
    String name,
    String systemPrompt,
    String tone,
    String language,
    String avatarUrl,
    boolean isDefault,
    Instant createdAt
) {}
