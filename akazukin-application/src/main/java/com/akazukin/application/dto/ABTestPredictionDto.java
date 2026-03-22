package com.akazukin.application.dto;

import java.util.List;

public record ABTestPredictionDto(
    long totalImpressions,
    long totalEngagements,
    double averageEngagementRate,
    List<VariantStat> variantStats,
    double confidencePercent,
    String aiVerdict
) {
    public record VariantStat(
        String variant,
        double engagementRate,
        double clickRate
    ) {}
}
