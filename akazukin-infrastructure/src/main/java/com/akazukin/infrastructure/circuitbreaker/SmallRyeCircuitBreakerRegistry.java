package com.akazukin.infrastructure.circuitbreaker;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.CircuitBreakerRegistry;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Alternative
@Priority(100)
@Named("smallRye")
public class SmallRyeCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private static final Logger LOG = Logger.getLogger(SmallRyeCircuitBreakerRegistry.class.getName());

    private final CircuitBreakerMaintenance maintenance;
    private final ConcurrentHashMap<SnsPlatform, Instant> lastFailureTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SnsPlatform, Integer> failureCounts = new ConcurrentHashMap<>();

    @Inject
    public SmallRyeCircuitBreakerRegistry(CircuitBreakerMaintenance maintenance) {
        this.maintenance = maintenance;
    }

    @Override
    public CircuitBreakerState getState(SnsPlatform platform) {
        String cbName = toCircuitBreakerName(platform);
        CircuitState state = readState(cbName);
        int failures = failureCounts.getOrDefault(platform, 0);
        Instant lastFailure = lastFailureTimes.get(platform);
        return new CircuitBreakerState(platform, state, failures, lastFailure);
    }

    @Override
    public List<CircuitBreakerState> getAllStates() {
        return Arrays.stream(SnsPlatform.values())
                .map(this::getState)
                .toList();
    }

    @Override
    public void recordSuccess(SnsPlatform platform) {
        failureCounts.put(platform, 0);
    }

    @Override
    public void recordFailure(SnsPlatform platform) {
        failureCounts.merge(platform, 1, Integer::sum);
        lastFailureTimes.put(platform, Instant.now());
    }

    @Override
    public boolean isCallPermitted(SnsPlatform platform) {
        String cbName = toCircuitBreakerName(platform);
        CircuitState state = readState(cbName);
        return state != CircuitState.OPEN;
    }

    private CircuitState readState(String cbName) {
        try {
            io.smallrye.faulttolerance.api.CircuitBreakerState srState = maintenance.currentState(cbName);
            return switch (srState) {
                case CLOSED -> CircuitState.CLOSED;
                case OPEN -> CircuitState.OPEN;
                case HALF_OPEN -> CircuitState.HALF_OPEN;
            };
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Circuit breaker not yet registered: {0}, returning CLOSED (initial state)", cbName);
            return CircuitState.CLOSED;
        }
    }

    static String toCircuitBreakerName(SnsPlatform platform) {
        return "sns-" + platform.name().toLowerCase();
    }
}
