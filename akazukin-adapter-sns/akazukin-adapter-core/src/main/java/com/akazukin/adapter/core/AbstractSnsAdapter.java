package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AdapterMetricsCollector;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import com.akazukin.domain.port.ExternalApiAuditPort;
import com.akazukin.domain.port.SnsAdapter;
import com.akazukin.domain.port.SnsAnalyticsAdapter;
import com.akazukin.domain.port.SnsGraphAdapter;
import com.akazukin.domain.port.SnsInteractionAdapter;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.exception.SnsApiException;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSnsAdapter implements SnsAdapter, SnsInteractionAdapter, SnsGraphAdapter, SnsAnalyticsAdapter {

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

    private static final long PROFILE_CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private static volatile ExternalApiAuditPort auditPort;

    private final RateLimiter rateLimiter;
    private final ConcurrentHashMap<String, ProfileCacheEntry> profileCache = new ConcurrentHashMap<>();

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private AdapterMetricsCollector metricsCollector;

    protected AbstractSnsAdapter() {
        this.rateLimiter = new RateLimiter();
    }

    public void setCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
        this.circuitBreakerRegistry = registry;
    }

    public void setAdapterMetricsCollector(AdapterMetricsCollector collector) {
        this.metricsCollector = collector;
    }

    public static void setAuditPort(ExternalApiAuditPort port) {
        auditPort = port;
    }

    protected void logExternalCall(String httpMethod, String endpoint, int responseStatus, long durationMs) {
        ExternalApiAuditPort port = auditPort;
        if (port != null) {
            port.logExternalApiCall(platform(), httpMethod, endpoint, responseStatus, durationMs, null, null);
        }
    }

    protected void checkCircuitBreaker() {
        if (circuitBreakerRegistry != null && !circuitBreakerRegistry.isCallPermitted(platform())) {
            throw new SnsApiException(platform(), 503, "Circuit breaker is open for " + platform());
        }
    }

    protected void recordSuccess(String operation, long latencyMs) {
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.recordSuccess(platform());
        }
        if (metricsCollector != null) {
            metricsCollector.recordSuccess(platform(), operation, latencyMs);
        }
    }

    protected void recordFailure(String operation, long latencyMs, Throwable cause) {
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.recordFailure(platform());
        }
        if (metricsCollector != null) {
            metricsCollector.recordFailure(platform(), operation, latencyMs, cause);
        }
    }

    /**
     * Returns a cached {@link SnsProfile} for the given access token, or {@code null}
     * if the cache entry does not exist or has expired (TTL: 5 minutes).
     * Expired entries are removed before returning.
     */
    protected SnsProfile getCachedProfile(String accessToken) {
        ProfileCacheEntry entry = profileCache.get(accessToken);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            profileCache.remove(accessToken);
            return null;
        }
        return entry.profile();
    }

    /**
     * Stores a {@link SnsProfile} in the cache keyed by access token.
     * Also evicts any expired entries to prevent unbounded growth.
     */
    protected void cacheProfile(String accessToken, SnsProfile profile) {
        profileCache.put(accessToken, new ProfileCacheEntry(profile, Instant.now()));
        evictExpiredProfiles();
    }

    private void evictExpiredProfiles() {
        Iterator<Map.Entry<String, ProfileCacheEntry>> it = profileCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ProfileCacheEntry> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private record ProfileCacheEntry(SnsProfile profile, Instant cachedAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() - cachedAt.toEpochMilli() > PROFILE_CACHE_TTL_MILLIS;
        }
    }

    protected void checkRateLimit() {
        rateLimiter.checkLimit(platform());
    }

    protected void recordApiCall() {
        rateLimiter.recordCall(platform());
    }

    /**
     * Checks the HTTP response status. If a 429 (rate limited) response is received,
     * throws a {@link RateLimitExceededException} immediately without waiting.
     * Retry/backoff responsibility belongs to the SDK layer, not the adapter layer.
     */
    protected void checkResponseStatus(HttpResponse<String> response, String operation) {
        if (response.statusCode() == HTTP_RATE_LIMITED) {
            String retryAfterStr = response.headers().firstValue("Retry-After").orElse(null);
            long retryAfterSeconds = parseRetryAfter(retryAfterStr);

            if (retryAfterSeconds > 0) {
                LOG.log(Level.WARNING,
                    "Rate limited by {0} during {1}. Retry-After: {2} seconds.",
                    new Object[]{platform(), operation, retryAfterSeconds});
                throw new RateLimitExceededException(platform(), retryAfterSeconds);
            }

            String retryDisplay = retryAfterStr != null ? retryAfterStr : "unknown";
            LOG.log(Level.WARNING,
                "Rate limited by {0} during {1}. Retry-After: {2}.",
                new Object[]{platform(), operation, retryDisplay});
            throw new RateLimitExceededException(platform(), -1);
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

    @Override
    public void reply(String accessToken, String postId, String content) {
        throw new UnsupportedOperationException(platform() + " does not support reply");
    }

    @Override
    public void mention(String accessToken, String userId, String content) {
        throw new UnsupportedOperationException(platform() + " does not support mention");
    }

    @Override
    public List<SnsProfile> getFollowers(String accessToken, int limit) {
        throw new UnsupportedOperationException(platform() + " does not support getFollowers");
    }

    @Override
    public List<SnsProfile> getFollowing(String accessToken, int limit) {
        throw new UnsupportedOperationException(platform() + " does not support getFollowing");
    }

    @Override
    public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
        return Optional.empty();
    }

    @Override
    public Optional<AccountStats> getAccountStats(String accessToken) {
        return Optional.empty();
    }

    protected void perfLog(String methodName, long startNanos) {
        long perfMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (perfMs >= 100) {
            LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{methodName, perfMs});
        } else {
            LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{methodName, perfMs});
        }
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
