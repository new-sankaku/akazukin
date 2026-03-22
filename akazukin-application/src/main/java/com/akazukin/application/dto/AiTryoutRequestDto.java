package com.akazukin.application.dto;

import java.util.UUID;

public record AiTryoutRequestDto(
    UUID personaId,
    String text
) {}
