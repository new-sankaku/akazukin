package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NewsItemDto(
    UUID id,
    UUID sourceId,
    String sourceName,
    String title,
    String url,
    String summary,
    Instant publishedAt
) {}
