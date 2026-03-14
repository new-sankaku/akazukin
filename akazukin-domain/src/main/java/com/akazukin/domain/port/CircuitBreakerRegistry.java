package com.akazukin.domain.port;

import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.SnsPlatform;

import java.util.List;

public interface CircuitBreakerRegistry {

    CircuitBreakerState getState(SnsPlatform platform);

    List<CircuitBreakerState> getAllStates();

    void recordSuccess(SnsPlatform platform);

    void recordFailure(SnsPlatform platform);
}
