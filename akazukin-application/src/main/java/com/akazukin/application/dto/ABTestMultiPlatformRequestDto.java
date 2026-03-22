package com.akazukin.application.dto;

import java.util.List;

public record ABTestMultiPlatformRequestDto(
    String name,
    String originalText,
    List<String> platforms
) {}
