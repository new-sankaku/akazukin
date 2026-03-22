package com.akazukin.application.dto;

import java.util.UUID;

public record PersonaReplyRequestDto(
    UUID replyId,
    UUID personaId,
    String originalContent
) {}
