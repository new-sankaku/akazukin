package com.akazukin.infrastructure.cache;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CachedAnalyticsService {

    private static final String CACHE_NAME = "analytics-summary";

    @CacheResult(cacheName = CACHE_NAME)
    public Map<String, Long> getAnalyticsSummary(UUID userId) {
        return Map.of();
    }

    @CacheInvalidate(cacheName = CACHE_NAME)
    public void invalidateAnalyticsSummary(UUID userId) {
    }
}
