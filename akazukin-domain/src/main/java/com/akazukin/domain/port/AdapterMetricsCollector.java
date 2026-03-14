package com.akazukin.domain.port;

import com.akazukin.domain.model.SnsPlatform;

public interface AdapterMetricsCollector {

    void recordSuccess(SnsPlatform platform, String operation, long latencyMillis);

    void recordFailure(SnsPlatform platform, String operation, long latencyMillis, Throwable cause);
}
