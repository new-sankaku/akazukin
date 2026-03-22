package com.akazukin.application.dto;

public record HeatmapCellDto(
    String theme,
    String platform,
    int score
) {}
