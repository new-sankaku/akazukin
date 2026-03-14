package com.akazukin.domain.model;

import java.time.Instant;

public record CircuitBreakerState(
        SnsPlatform platform,
        CircuitState state,
        int failureCount,
        Instant lastFailureTime
) {
}
