package com.akazukin.infrastructure.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedAnalyticsServiceTest {

    @Test
    void shouldReturnEmptyMapForNewUser() {
        CachedAnalyticsService service = new CachedAnalyticsService();
        Map<String, Long> result = service.getAnalyticsSummary(UUID.randomUUID());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
