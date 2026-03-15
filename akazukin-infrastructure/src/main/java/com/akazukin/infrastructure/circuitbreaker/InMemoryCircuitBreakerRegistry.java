package com.akazukin.infrastructure.circuitbreaker;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.CircuitBreakerRegistry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@Named("inMemory")
public class InMemoryCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_DURATION_MILLIS = 60_000;

    private final ConcurrentHashMap<SnsPlatform, PlatformCircuitState> states = new ConcurrentHashMap<>();

    @Override
    public CircuitBreakerState getState(SnsPlatform platform) {
        PlatformCircuitState pcs = states.computeIfAbsent(platform, p -> new PlatformCircuitState());
        return pcs.toCircuitBreakerState(platform);
    }

    @Override
    public List<CircuitBreakerState> getAllStates() {
        return Arrays.stream(SnsPlatform.values())
                .map(this::getState)
                .toList();
    }

    @Override
    public void recordSuccess(SnsPlatform platform) {
        PlatformCircuitState pcs = states.computeIfAbsent(platform, p -> new PlatformCircuitState());
        pcs.reset();
    }

    @Override
    public void recordFailure(SnsPlatform platform) {
        PlatformCircuitState pcs = states.computeIfAbsent(platform, p -> new PlatformCircuitState());
        pcs.recordFailure();
    }

    @Override
    public boolean isCallPermitted(SnsPlatform platform) {
        CircuitBreakerState cbState = getState(platform);
        return cbState.state() != CircuitState.OPEN;
    }

    private static final class PlatformCircuitState {

        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(null);
        private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);

        void recordFailure() {
            int count = failureCount.incrementAndGet();
            lastFailureTime.set(Instant.now());
            if (count >= FAILURE_THRESHOLD) {
                state.set(CircuitState.OPEN);
            }
        }

        void reset() {
            failureCount.set(0);
            state.set(CircuitState.CLOSED);
        }

        CircuitBreakerState toCircuitBreakerState(SnsPlatform platform) {
            CircuitState current = state.get();
            Instant lastFailure = lastFailureTime.get();

            if (current == CircuitState.OPEN && lastFailure != null) {
                long elapsed = Instant.now().toEpochMilli() - lastFailure.toEpochMilli();
                if (elapsed > OPEN_DURATION_MILLIS) {
                    state.set(CircuitState.HALF_OPEN);
                    current = CircuitState.HALF_OPEN;
                }
            }

            return new CircuitBreakerState(
                    platform,
                    current,
                    failureCount.get(),
                    lastFailure
            );
        }
    }
}
