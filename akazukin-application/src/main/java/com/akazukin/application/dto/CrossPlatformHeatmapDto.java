package com.akazukin.application.dto;

import java.util.List;

public record CrossPlatformHeatmapDto(
    List<String> themes,
    List<String> platforms,
    List<HeatmapCellDto> cells
) {}
