package com.akazukin.application.dto;

public record CrossPostRecommendDto(
    String platform,
    String reason,
    int score,
    boolean contentConverted
) {}
