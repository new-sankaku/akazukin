package com.akazukin.application.dto;

import java.time.LocalDate;

public record CalendarTimelineEventDto(
    LocalDate date,
    String name,
    String type,
    String hint
) {
}
