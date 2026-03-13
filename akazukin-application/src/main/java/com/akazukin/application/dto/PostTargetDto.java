package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PostTargetDto(
    UUID id,
    String platform,
    String status,
    String platformPostId,
    String errorMessage,
    Instant publishedAt
) {}
