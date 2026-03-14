package com.akazukin.infrastructure.metrics;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AdapterMetricsCollector;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@ApplicationScoped
public class InMemoryAdapterMetricsCollector implements AdapterMetricsCollector {

    private final Map<String, LongAdder> successCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> totalLatencyMillis = new ConcurrentHashMap<>();

    @Override
    public void recordSuccess(SnsPlatform platform, String operation, long latencyMillis) {
        String key = platform.name() + "." + operation;
        successCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
        totalLatencyMillis.computeIfAbsent(key, k -> new LongAdder()).add(latencyMillis);
    }

    @Override
    public void recordFailure(SnsPlatform platform, String operation, long latencyMillis, Throwable cause) {
        String key = platform.name() + "." + operation;
        failureCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
        totalLatencyMillis.computeIfAbsent(key, k -> new LongAdder()).add(latencyMillis);
    }

    public long getSuccessCount(SnsPlatform platform, String operation) {
        String key = platform.name() + "." + operation;
        LongAdder adder = successCounters.get(key);
        return adder != null ? adder.sum() : 0;
    }

    public long getFailureCount(SnsPlatform platform, String operation) {
        String key = platform.name() + "." + operation;
        LongAdder adder = failureCounters.get(key);
        return adder != null ? adder.sum() : 0;
    }

    public long getTotalLatencyMillis(SnsPlatform platform, String operation) {
        String key = platform.name() + "." + operation;
        LongAdder adder = totalLatencyMillis.get(key);
        return adder != null ? adder.sum() : 0;
    }
}
