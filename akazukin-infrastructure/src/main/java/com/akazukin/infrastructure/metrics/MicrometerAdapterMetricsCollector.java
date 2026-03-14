package com.akazukin.infrastructure.metrics;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AdapterMetricsCollector;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Duration;

@ApplicationScoped
@Alternative
@Priority(100)
public class MicrometerAdapterMetricsCollector implements AdapterMetricsCollector {

    private final MeterRegistry registry;

    @Inject
    public MicrometerAdapterMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordSuccess(SnsPlatform platform, String operation, long latencyMillis) {
        Counter.builder("akazukin.adapter.requests")
                .tag("platform", platform.name().toLowerCase())
                .tag("operation", operation)
                .tag("result", "success")
                .register(registry)
                .increment();

        Timer.builder("akazukin.adapter.latency")
                .tag("platform", platform.name().toLowerCase())
                .tag("operation", operation)
                .register(registry)
                .record(Duration.ofMillis(latencyMillis));
    }

    @Override
    public void recordFailure(SnsPlatform platform, String operation, long latencyMillis, Throwable cause) {
        Counter.builder("akazukin.adapter.requests")
                .tag("platform", platform.name().toLowerCase())
                .tag("operation", operation)
                .tag("result", "failure")
                .tag("error", cause.getClass().getSimpleName())
                .register(registry)
                .increment();

        Timer.builder("akazukin.adapter.latency")
                .tag("platform", platform.name().toLowerCase())
                .tag("operation", operation)
                .register(registry)
                .record(Duration.ofMillis(latencyMillis));
    }
}
