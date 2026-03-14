package com.akazukin.application.dto;

import java.util.List;

public record PostTemplateRequestDto(
    String name,
    String content,
    List<String> placeholders,
    List<String> platforms,
    String category
) {}
