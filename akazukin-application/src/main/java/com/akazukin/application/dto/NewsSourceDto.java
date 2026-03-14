package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NewsSourceDto(
    UUID id,
    String name,
    String url,
    String sourceType,
    boolean isActive,
    Instant createdAt
) {}
