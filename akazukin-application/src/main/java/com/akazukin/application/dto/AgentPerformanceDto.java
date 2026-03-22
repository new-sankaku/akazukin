package com.akazukin.application.dto;

public record AgentPerformanceDto(
    String agentType,
    long totalTasks,
    long completedTasks,
    long failedTasks,
    double successRate,
    double avgDurationMs
) {}
