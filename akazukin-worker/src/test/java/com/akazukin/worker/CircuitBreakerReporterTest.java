package com.akazukin.worker;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerReporterTest {

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreakerReporter circuitBreakerReporter;

    @BeforeEach
    void setUp() {
        circuitBreakerReporter = new CircuitBreakerReporter(circuitBreakerRegistry);
    }

    @Test
    void reportCircuitBreakerStatus_handlesEmptyStateList() {
        when(circuitBreakerRegistry.getAllStates()).thenReturn(List.of());

        circuitBreakerReporter.reportCircuitBreakerStatus();

        verify(circuitBreakerRegistry).getAllStates();
    }

    @Test
    void reportCircuitBreakerStatus_reportsAllClosedCircuits() {
        CircuitBreakerState closedState = new CircuitBreakerState(
                SnsPlatform.TWITTER, CircuitState.CLOSED, 0, null);
        when(circuitBreakerRegistry.getAllStates()).thenReturn(List.of(closedState));

        circuitBreakerReporter.reportCircuitBreakerStatus();

        verify(circuitBreakerRegistry).getAllStates();
    }

    @Test
    void reportCircuitBreakerStatus_reportsOpenCircuits() {
        CircuitBreakerState openState = new CircuitBreakerState(
                SnsPlatform.TWITTER, CircuitState.OPEN, 5, Instant.now());
        CircuitBreakerState closedState = new CircuitBreakerState(
                SnsPlatform.BLUESKY, CircuitState.CLOSED, 0, null);
        when(circuitBreakerRegistry.getAllStates())
                .thenReturn(List.of(openState, closedState));

        circuitBreakerReporter.reportCircuitBreakerStatus();

        verify(circuitBreakerRegistry).getAllStates();
    }

    @Test
    void reportCircuitBreakerStatus_reportsHalfOpenCircuits() {
        CircuitBreakerState halfOpenState = new CircuitBreakerState(
                SnsPlatform.MASTODON, CircuitState.HALF_OPEN, 3, Instant.now());
        when(circuitBreakerRegistry.getAllStates())
                .thenReturn(List.of(halfOpenState));

        circuitBreakerReporter.reportCircuitBreakerStatus();

        verify(circuitBreakerRegistry).getAllStates();
    }

    @Test
    void reportCircuitBreakerStatus_reportsMultipleOpenCircuits() {
        CircuitBreakerState open1 = new CircuitBreakerState(
                SnsPlatform.TWITTER, CircuitState.OPEN, 10, Instant.now());
        CircuitBreakerState open2 = new CircuitBreakerState(
                SnsPlatform.INSTAGRAM, CircuitState.HALF_OPEN, 2, Instant.now());
        CircuitBreakerState closed = new CircuitBreakerState(
                SnsPlatform.BLUESKY, CircuitState.CLOSED, 0, null);
        when(circuitBreakerRegistry.getAllStates())
                .thenReturn(List.of(open1, open2, closed));

        circuitBreakerReporter.reportCircuitBreakerStatus();

        verify(circuitBreakerRegistry).getAllStates();
    }
}
