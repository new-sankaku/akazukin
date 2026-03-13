package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;

public class RateLimitExceededException extends RuntimeException {

    private final SnsPlatform platform;
    private final long retryAfterSeconds;

    public RateLimitExceededException(SnsPlatform platform, long retryAfterSeconds) {
        super("Rate limit exceeded for " + platform.name() + ". Retry after " + retryAfterSeconds + " seconds.");
        this.platform = platform;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public SnsPlatform getPlatform() {
        return platform;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
