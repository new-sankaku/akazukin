package com.akazukin.web.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class RateLimitBucketManager {

    private static final int MAX_ENTRIES = 10_000;
    private static final Duration EXPIRY = Duration.ofHours(1);

    private final ConcurrentMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final RateLimitConfig config;

    @Inject
    public RateLimitBucketManager(RateLimitConfig config) {
        this.config = config;
    }

    public ConsumptionProbe tryConsume(String userIdentifier, EndpointCategory category) {
        String key = userIdentifier + ":" + category.name();
        BucketEntry entry = buckets.compute(key, (k, existing) -> {
            if (existing != null) {
                existing.touch();
                return existing;
            }
            if (buckets.size() >= MAX_ENTRIES) {
                return null;
            }
            return new BucketEntry(createBucket(category));
        });
        if (entry == null) {
            return createBucket(category).tryConsumeAndReturnRemaining(1);
        }
        return entry.bucket().tryConsumeAndReturnRemaining(1);
    }

    @Scheduled(every = "1h", identity = "rate-limit-bucket-cleanup")
    void cleanupExpiredEntries() {
        Instant cutoff = Instant.now().minus(EXPIRY);
        for (Map.Entry<String, BucketEntry> entry : buckets.entrySet()) {
            if (entry.getValue().lastAccess().isBefore(cutoff)) {
                buckets.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private Bucket createBucket(EndpointCategory category) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(config.getCapacity(category))
                .refillGreedy(config.getCapacity(category), config.getPeriod(category))
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        Bucket bucket() {
            return bucket;
        }

        Instant lastAccess() {
            return lastAccess;
        }

        void touch() {
            this.lastAccess = Instant.now();
        }
    }
}
