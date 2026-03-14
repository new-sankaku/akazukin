package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;

public record CalendarEntryRequestDto(
    String title,
    String description,
    Instant scheduledAt,
    List<String> platforms,
    String color
) {}
