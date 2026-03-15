package com.akazukin.infrastructure.metrics;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AdapterMetricsCollector;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
@Alternative
@Priority(100)
public class MicrometerAdapterMetricsCollector implements AdapterMetricsCollector {

    private static final Set<String> KNOWN_ERROR_NAMES = Set.of(
            "RateLimitExceededException",
            "RetryableRateLimitException"
    );

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    @Inject
    public MicrometerAdapterMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordSuccess(SnsPlatform platform, String operation, long latencyMillis) {
        String platformTag = platform.name().toLowerCase(Locale.ROOT);

        getOrCreateCounter(platformTag, operation, "success", "none").increment();
        getOrCreateTimer(platformTag, operation).record(Duration.ofMillis(latencyMillis));
    }

    @Override
    public void recordFailure(SnsPlatform platform, String operation, long latencyMillis, Throwable cause) {
        String platformTag = platform.name().toLowerCase(Locale.ROOT);
        String errorTag = normalizeErrorClass(cause);

        getOrCreateCounter(platformTag, operation, "failure", errorTag).increment();
        getOrCreateTimer(platformTag, operation).record(Duration.ofMillis(latencyMillis));
    }

    private String normalizeErrorClass(Throwable cause) {
        if (cause instanceof SnsApiException) {
            return "SnsApiException";
        }
        if (cause instanceof HttpTimeoutException) {
            return "HttpTimeoutException";
        }
        if (cause instanceof IOException) {
            return "IOException";
        }
        if (cause instanceof IllegalArgumentException) {
            return "IllegalArgumentException";
        }
        if (cause instanceof IllegalStateException) {
            return "IllegalStateException";
        }
        if (KNOWN_ERROR_NAMES.contains(cause.getClass().getSimpleName())) {
            return cause.getClass().getSimpleName();
        }
        return "other";
    }

    private Counter getOrCreateCounter(String platform, String operation, String result, String error) {
        String key = platform + "|" + operation + "|" + result + "|" + error;
        return counterCache.computeIfAbsent(key, k ->
                Counter.builder("akazukin.adapter.requests")
                        .tag("platform", platform)
                        .tag("operation", operation)
                        .tag("result", result)
                        .tag("error", error)
                        .register(registry)
        );
    }

    private Timer getOrCreateTimer(String platform, String operation) {
        String key = platform + "|" + operation;
        return timerCache.computeIfAbsent(key, k ->
                Timer.builder("akazukin.adapter.latency")
                        .tag("platform", platform)
                        .tag("operation", operation)
                        .register(registry)
        );
    }
}
