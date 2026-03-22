package com.akazukin.application.dto;

import java.util.List;

public record AiPlanRequestDto(
    String theme,
    List<String> platforms,
    int postCount
) {
}
