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
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSnsAdapter implements SnsAdapter {

    private static final Logger LOG = Logger.getLogger(AbstractSnsAdapter.class.getName());

    protected static final int HTTP_CLIENT_ERROR = 400;
    protected static final int HTTP_RATE_LIMITED = 429;
    protected static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    protected static final int TWITTER_MAX_LENGTH = 280;
    protected static final int BLUESKY_MAX_LENGTH = 300;
    protected static final int MASTODON_MAX_LENGTH = 500;
    protected static final int REDDIT_MAX_LENGTH = 40000;
    protected static final int DEFAULT_MAX_LENGTH = 2200;

    private static final long MAX_RETRY_AFTER_SECONDS = 60;

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

    /**
     * Checks the HTTP response status. If a 429 (rate limited) response is received
     * and the Retry-After header indicates a wait of 60 seconds or less, this method
     * sleeps for the indicated duration and throws a {@link RetryableRateLimitException}
     * to signal the caller should retry. If the Retry-After value exceeds 60 seconds
     * or is not parseable, a {@link SnsApiException} is thrown immediately.
     */
    protected void checkResponseStatus(HttpResponse<String> response, String operation) {
        if (response.statusCode() == HTTP_RATE_LIMITED) {
            String retryAfterStr = response.headers().firstValue("Retry-After").orElse(null);
            long retryAfterSeconds = parseRetryAfter(retryAfterStr);

            if (retryAfterSeconds > 0 && retryAfterSeconds <= MAX_RETRY_AFTER_SECONDS) {
                LOG.log(Level.WARNING,
                    "Rate limited by {0} during {1}. Waiting {2} seconds before retry.",
                    new Object[]{platform(), operation, retryAfterSeconds});
                try {
                    Thread.sleep(retryAfterSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SnsApiException(platform(), HTTP_RATE_LIMITED,
                        "Rate limit wait interrupted during " + operation);
                }
                throw new RetryableRateLimitException(platform(), retryAfterSeconds);
            }

            String retryDisplay = retryAfterStr != null ? retryAfterStr : "unknown";
            throw new SnsApiException(platform(), response.statusCode(),
                "Rate limit exceeded. Retry after " + retryDisplay + " seconds");
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

    private long parseRetryAfter(String retryAfterStr) {
        if (retryAfterStr == null || retryAfterStr.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(retryAfterStr.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
