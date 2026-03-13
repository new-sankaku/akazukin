package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RateLimiter {

    private static final int DEFAULT_MAX_CALLS = 100;
    private static final long WINDOW_SECONDS = 900; // 15 minutes

    private final ConcurrentHashMap<SnsPlatform, ConcurrentLinkedQueue<Instant>> callHistory =
            new ConcurrentHashMap<>();

    public void checkLimit(SnsPlatform platform) {
        cleanup(platform);
        ConcurrentLinkedQueue<Instant> history =
                callHistory.computeIfAbsent(platform, k -> new ConcurrentLinkedQueue<>());
        if (history.size() >= DEFAULT_MAX_CALLS) {
            throw new RateLimitExceededException(platform, WINDOW_SECONDS);
        }
    }

    public void recordCall(SnsPlatform platform) {
        callHistory.computeIfAbsent(platform, k -> new ConcurrentLinkedQueue<>()).add(Instant.now());
    }

    private void cleanup(SnsPlatform platform) {
        ConcurrentLinkedQueue<Instant> history = callHistory.get(platform);
        if (history == null) return;
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        history.removeIf(instant -> instant.isBefore(cutoff));
    }
}
