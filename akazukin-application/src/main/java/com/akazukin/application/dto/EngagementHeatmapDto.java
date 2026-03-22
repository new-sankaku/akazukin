package com.akazukin.application.dto;

import java.time.LocalDate;
import java.util.Map;

public record EngagementHeatmapDto(
    Map<LocalDate, Integer> levels
) {
}
