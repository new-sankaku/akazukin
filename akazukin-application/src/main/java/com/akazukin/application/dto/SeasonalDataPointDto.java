package com.akazukin.application.dto;

public record SeasonalDataPointDto(
    int month,
    String platform,
    double engagementRate,
    boolean isPreviousYear
) {}
