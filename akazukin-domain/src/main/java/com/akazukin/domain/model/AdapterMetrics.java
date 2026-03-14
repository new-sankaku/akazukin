package com.akazukin.domain.model;

public record AdapterMetrics(
    SnsPlatform platform,
    long successCount,
    long failureCount,
    double avgLatencyMs,
    double p99LatencyMs
) {
}
