package com.akazukin.worker;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CircuitBreakerReporter {

    private static final Logger LOG = Logger.getLogger(CircuitBreakerReporter.class.getName());

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Inject
    public CircuitBreakerReporter(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Scheduled(every = "5m", identity = "circuit-breaker-reporter")
    void reportCircuitBreakerStatus() {
        List<CircuitBreakerState> allStates = circuitBreakerRegistry.getAllStates();
        if (allStates.isEmpty()) {
            LOG.log(Level.FINE, "No circuit breaker states registered");
            return;
        }

        List<CircuitBreakerState> openCircuits = allStates.stream()
                .filter(s -> s.state() != CircuitState.CLOSED)
                .toList();

        if (openCircuits.isEmpty()) {
            LOG.log(Level.FINE, "All {0} circuit breakers are CLOSED",
                    allStates.size());
        } else {
            for (CircuitBreakerState state : openCircuits) {
                LOG.log(Level.WARNING,
                        "Circuit breaker {0}: state={1}, failures={2}, lastFailure={3}",
                        new Object[]{state.platform(), state.state(),
                                state.failureCount(), state.lastFailureTime()});
            }
        }
    }
}
