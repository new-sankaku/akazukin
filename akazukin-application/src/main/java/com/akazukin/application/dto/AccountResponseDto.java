package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponseDto(
    UUID id,
    String platform,
    String accountIdentifier,
    String displayName,
    Instant connectedAt
) {}
