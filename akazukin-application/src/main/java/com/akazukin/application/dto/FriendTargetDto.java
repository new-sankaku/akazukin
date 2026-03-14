package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record FriendTargetDto(
    UUID id,
    String platform,
    String targetIdentifier,
    String displayName,
    String notes,
    Instant createdAt
) {
}
