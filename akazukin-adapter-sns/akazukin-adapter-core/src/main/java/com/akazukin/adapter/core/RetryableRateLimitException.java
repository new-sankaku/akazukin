package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;

/**
 * Thrown after a short wait when an HTTP 429 response has a Retry-After value
 * that is within the acceptable retry window. Callers can catch this exception
 * and retry the operation.
 */
public class RetryableRateLimitException extends RateLimitExceededException {

    public RetryableRateLimitException(SnsPlatform platform, long retryAfterSeconds) {
        super(platform, retryAfterSeconds);
    }
}
