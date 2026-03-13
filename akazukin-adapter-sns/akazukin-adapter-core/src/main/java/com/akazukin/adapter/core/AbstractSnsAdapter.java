package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAdapter;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.exception.SnsApiException;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public abstract class AbstractSnsAdapter implements SnsAdapter {

    protected static final int HTTP_CLIENT_ERROR = 400;
    protected static final int HTTP_RATE_LIMITED = 429;
    protected static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    protected static final int TWITTER_MAX_LENGTH = 280;
    protected static final int BLUESKY_MAX_LENGTH = 300;
    protected static final int MASTODON_MAX_LENGTH = 500;
    protected static final int REDDIT_MAX_LENGTH = 40000;
    protected static final int DEFAULT_MAX_LENGTH = 2200;

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

    protected void checkResponseStatus(HttpResponse<String> response, String operation) {
        if (response.statusCode() == HTTP_RATE_LIMITED) {
            String retryAfter = response.headers().firstValue("Retry-After").orElse("unknown");
            throw new SnsApiException(platform(), response.statusCode(),
                "Rate limit exceeded. Retry after " + retryAfter + " seconds");
        }
        if (response.statusCode() >= HTTP_CLIENT_ERROR) {
            throw new SnsApiException(platform(), response.statusCode(),
                "HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    @Override
    public int getMaxContentLength() {
        return DEFAULT_MAX_LENGTH;
    }

    protected SnsApiException wrapException(String operation, Exception cause) {
        return new SnsApiException(
            platform(),
            0,
            operation + " failed: " + cause.getMessage()
        );
    }
}
