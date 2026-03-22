package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record FireWatchPostDto(
    UUID postTargetId,
    String platform,
    String content,
    Instant publishedAt,
    String severity,
    double engagementRate,
    double engagementDelta,
    long impressionsCount
) {}
