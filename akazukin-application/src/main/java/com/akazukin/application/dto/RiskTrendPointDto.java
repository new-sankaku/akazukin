package com.akazukin.application.dto;

public record RiskTrendPointDto(
    String month,
    double detectionRate,
    String severity
) {}
