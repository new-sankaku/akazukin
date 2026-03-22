package com.akazukin.application.dto;

import java.util.List;
import java.util.Map;

public record TimeSlotMatrixDto(
    List<String> platforms,
    List<String> dayLabels,
    List<String> hourLabels,
    Map<String, Map<String, Integer>> engagementLevels
) {
}
