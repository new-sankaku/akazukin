package com.akazukin.web.health;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.port.CircuitBreakerRegistry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.util.List;

@Readiness
@ApplicationScoped
public class SnsApiHealthCheck implements HealthCheck {

    private static final String HEALTH_CHECK_NAME = "SNS API circuit breakers";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Inject
    public SnsApiHealthCheck(@Named("smallRye") CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(HEALTH_CHECK_NAME);

        List<CircuitBreakerState> states = circuitBreakerRegistry.getAllStates();

        boolean allHealthy = true;
        int openCount = 0;

        for (CircuitBreakerState state : states) {
            String platformName = state.platform().name().toLowerCase();
            builder.withData(platformName, state.state().name());

            if (state.state() == CircuitState.OPEN) {
                allHealthy = false;
                openCount++;
                if (state.lastFailureTime() != null) {
                    builder.withData(platformName + "-lastFailure", state.lastFailureTime().toString());
                }
                builder.withData(platformName + "-failureCount", state.failureCount());
            }
        }

        if (allHealthy) {
            builder.up();
        } else {
            builder.down()
                    .withData("openCircuitBreakers", openCount);
        }

        return builder.build();
    }
}
