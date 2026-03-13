package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAdapter;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.exception.SnsApiException;

import java.util.Objects;

public abstract class AbstractSnsAdapter implements SnsAdapter {

    private final RateLimiter rateLimiter;

    protected AbstractSnsAdapter() {
        this.rateLimiter = new RateLimiter();
    }

    protected void checkRateLimit() {
        rateLimiter.checkLimit(platform());
    }

    protected void recordApiCall() {
        rateLimiter.recordCall(platform());
    }

    protected SnsApiException wrapException(String operation, Exception cause) {
        return new SnsApiException(
            platform(),
            0,
            operation + " failed: " + cause.getMessage()
        );
    }
}
