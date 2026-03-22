package com.akazukin.application.dto;

import java.util.List;

public record PlatformCorrelationDto(
    List<String> platforms,
    List<CorrelationCellDto> cells,
    List<PlatformRecommendationDto> recommendations
) {}
