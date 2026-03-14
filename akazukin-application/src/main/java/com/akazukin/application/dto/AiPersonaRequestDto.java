package com.akazukin.application.dto;

public record AiPersonaRequestDto(
    String name,
    String systemPrompt,
    String tone,
    String language,
    String avatarUrl,
    boolean isDefault
) {}
