package com.akazukin.application.dto;

public record PersonaPerformanceDto(
    String personaName,
    long postCount,
    double avgEngagementRate,
    String trend
) {}
