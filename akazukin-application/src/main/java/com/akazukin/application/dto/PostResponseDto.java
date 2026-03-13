package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponseDto(
    UUID id,
    String content,
    String status,
    Instant scheduledAt,
    Instant createdAt,
    List<PostTargetDto> targets
) {}
