package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String type,
    String title,
    String body,
    UUID relatedEntityId,
    boolean isRead,
    Instant createdAt
) {}
