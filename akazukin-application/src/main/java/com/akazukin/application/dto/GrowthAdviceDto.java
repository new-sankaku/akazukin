package com.akazukin.application.dto;

public record GrowthAdviceDto(
    String platform,
    String advice,
    String currentFollowers,
    String projectedGrowth
) {
}
