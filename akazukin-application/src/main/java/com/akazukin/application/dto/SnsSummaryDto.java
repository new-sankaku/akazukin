package com.akazukin.application.dto;

public record SnsSummaryDto(
    String platform,
    int avgScore,
    int friendCount
) {}
