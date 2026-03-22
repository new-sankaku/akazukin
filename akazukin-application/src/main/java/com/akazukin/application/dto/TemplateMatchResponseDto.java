package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record TemplateMatchResponseDto(
    UUID newsItemId,
    String newsTitle,
    List<TemplateMatchItemDto> matches
) {}
