package com.akazukin.application.dto;

import java.util.UUID;

public record NewsPostIdeaRequestDto(
    UUID newsItemId,
    UUID personaId
) {}
