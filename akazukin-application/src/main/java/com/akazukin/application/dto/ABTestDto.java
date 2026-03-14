package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ABTestDto(
    UUID id,
    String name,
    String variantA,
    String variantB,
    String status,
    Instant startedAt,
    Instant completedAt,
    String winnerVariant,
    Instant createdAt
) {}
