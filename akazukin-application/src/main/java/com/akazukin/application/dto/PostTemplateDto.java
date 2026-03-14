package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record PostTemplateDto(
    UUID id,
    String name,
    String content,
    List<String> placeholders,
    List<String> platforms,
    String category,
    int usageCount
) {}
