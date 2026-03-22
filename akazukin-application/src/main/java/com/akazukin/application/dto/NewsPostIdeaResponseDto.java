package com.akazukin.application.dto;

import java.util.UUID;

public record NewsPostIdeaResponseDto(
    UUID newsItemId,
    String newsTitle,
    String personaName,
    String generatedText
) {}
