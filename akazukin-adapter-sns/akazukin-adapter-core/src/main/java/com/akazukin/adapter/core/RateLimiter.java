package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimiter {

    private static final int DEFAULT_WINDOW_SECONDS = 900;
    private static final int DEFAULT_MAX_CALLS = 100;
    private static final int MAX_QUEUE_SIZE = 1000;

    private static final Map<SnsPlatform, Integer> PLATFORM_LIMITS = Map.of(
        SnsPlatform.TWITTER, 50,
        SnsPlatform.INSTAGRAM, 200,
        SnsPlatform.THREADS, 200,
        SnsPlatform.BLUESKY, 100,
        SnsPlatform.MASTODON, 300,
        SnsPlatform.REDDIT, 60,
        SnsPlatform.TELEGRAM, 30,
        SnsPlatform.VK, 100,
        SnsPlatform.PINTEREST, 100
    );

    private final ConcurrentHashMap<SnsPlatform, ConcurrentLinkedDeque<Instant>> callHistory =
            new ConcurrentHashMap<>();

    public void checkLimit(SnsPlatform platform) {
        cleanup(platform);
        ConcurrentLinkedDeque<Instant> history =
                callHistory.computeIfAbsent(platform, k -> new ConcurrentLinkedDeque<>());
        int maxCalls = PLATFORM_LIMITS.getOrDefault(platform, DEFAULT_MAX_CALLS);
        if (history.size() >= maxCalls) {
            throw new RateLimitExceededException(platform, DEFAULT_WINDOW_SECONDS);
        }
    }

    public void recordCall(SnsPlatform platform) {
        ConcurrentLinkedDeque<Instant> history =
                callHistory.computeIfAbsent(platform, k -> new ConcurrentLinkedDeque<>());
        history.addLast(Instant.now());
        // Evict oldest if over max size
        while (history.size() > MAX_QUEUE_SIZE) {
            history.pollFirst();
        }
        cleanup(platform);
    }

    private void cleanup(SnsPlatform platform) {
        ConcurrentLinkedDeque<Instant> history = callHistory.get(platform);
        if (history == null) return;
        Instant cutoff = Instant.now().minusSeconds(DEFAULT_WINDOW_SECONDS);
        while (!history.isEmpty() && history.peekFirst().isBefore(cutoff)) {
            history.pollFirst();
        }
    }
}
