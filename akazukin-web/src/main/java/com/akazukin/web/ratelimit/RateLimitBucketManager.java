package com.akazukin.web.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class RateLimitBucketManager {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitConfig config;

    @Inject
    public RateLimitBucketManager(RateLimitConfig config) {
        this.config = config;
    }

    public ConsumptionProbe tryConsume(String userIdentifier, EndpointCategory category) {
        String key = userIdentifier + ":" + category.name();
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(category));
        return bucket.tryConsumeAndReturnRemaining(1);
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
}
