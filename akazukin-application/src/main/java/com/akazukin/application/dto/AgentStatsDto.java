package com.akazukin.application.dto;

public record AgentStatsDto(
    long totalExecutions,
    double averageDurationMs,
    int activeAgentCount,
    double successRate
) {
}
