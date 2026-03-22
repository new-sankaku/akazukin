package com.akazukin.application.dto;

import java.util.List;

public record ABTestWinPatternDto(
    int totalTestsAnalyzed,
    List<Pattern> patterns,
    String aiSummary
) {
    public record Pattern(
        String patternName,
        int winCount,
        double winRate,
        String description
    ) {}
}
