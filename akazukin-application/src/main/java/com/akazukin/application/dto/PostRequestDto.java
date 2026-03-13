package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;

public record PostRequestDto(
    String content,
    List<String> mediaUrls,
    List<String> targetPlatforms,
    Instant scheduledAt
) {}
