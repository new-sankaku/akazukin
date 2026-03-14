package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CalendarEntryDto(
    UUID id,
    UUID postId,
    String title,
    String description,
    Instant scheduledAt,
    List<String> platforms,
    String color
) {}
