package com.akazukin.adapter.core;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.CircuitState;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import com.akazukin.domain.port.SnsAdapter;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class FaultTolerantSnsAdapter implements SnsAdapter {

    private final SnsAdapter delegate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public FaultTolerantSnsAdapter(SnsAdapter delegate, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate adapter must not be null");
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public SnsPlatform platform() {
        return delegate.platform();
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public String getAuthorizationUrl(String callbackUrl, String state) {
        checkCircuitState();
        try {
            String result = delegate.getAuthorizationUrl(callbackUrl, state);
            recordSuccess();
            return result;
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        checkCircuitState();
        try {
            SnsAuthToken result = delegate.exchangeToken(code, callbackUrl);
            recordSuccess();
            return result;
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public SnsAuthToken refreshToken(String refreshToken) {
        checkCircuitState();
        try {
            SnsAuthToken result = delegate.refreshToken(refreshToken);
            recordSuccess();
            return result;
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public SnsProfile getProfile(String accessToken) {
        checkCircuitState();
        try {
            SnsProfile result = delegate.getProfile(accessToken);
            recordSuccess();
            return result;
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public PostResult post(String accessToken, PostRequest request) {
        checkCircuitState();
        try {
            PostResult result = delegate.post(accessToken, request);
            recordSuccess();
            return result;
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = SnsApiException.class
    )
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            retryOn = SnsApiException.class,
            abortOn = IllegalArgumentException.class
    )
    @ExponentialBackoff(maxDelay = 30, maxDelayUnit = ChronoUnit.SECONDS)
    public void deletePost(String accessToken, String postId) {
        checkCircuitState();
        try {
            delegate.deletePost(accessToken, postId);
            recordSuccess();
        } catch (SnsApiException e) {
            recordFailure();
            throw e;
        }
    }

    @Override
    public int getMaxContentLength() {
        return delegate.getMaxContentLength();
    }

    public SnsAdapter getDelegate() {
        return delegate;
    }

    private void checkCircuitState() {
        if (circuitBreakerRegistry == null) {
            return;
        }
        CircuitBreakerState cbState = circuitBreakerRegistry.getState(delegate.platform());
        if (cbState.state() == CircuitState.OPEN) {
            throw new SnsApiException(delegate.platform(), 0,
                    "Circuit breaker is OPEN for " + delegate.platform().name());
        }
    }

    private void recordSuccess() {
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.recordSuccess(delegate.platform());
        }
    }

    private void recordFailure() {
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.recordFailure(delegate.platform());
        }
    }
}
