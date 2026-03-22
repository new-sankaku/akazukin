package com.akazukin.application.dto;

public record PlatformRecommendationDto(
    String platform,
    String reason,
    String evidence
) {}
