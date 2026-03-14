package com.akazukin.web.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitBucketManagerTest {

    private RateLimitBucketManager manager;

    @BeforeEach
    void setUp() {
        RateLimitConfig config = new RateLimitConfig();
        config.generalCapacity = 5;
        config.generalPeriod = Duration.ofMinutes(1);
        config.postCapacity = 3;
        config.postPeriod = Duration.ofMinutes(1);
        config.aiCapacity = 2;
        config.aiPeriod = Duration.ofMinutes(1);
        manager = new RateLimitBucketManager(config);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.GENERAL);
            assertTrue(probe.isConsumed());
        }
    }

    @Test
    void shouldRejectRequestsExceedingLimit() {
        for (int i = 0; i < 5; i++) {
            manager.tryConsume("user1", EndpointCategory.GENERAL);
        }
        ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.GENERAL);
        assertFalse(probe.isConsumed());
    }

    @Test
    void shouldTrackDifferentUsersIndependently() {
        for (int i = 0; i < 5; i++) {
            manager.tryConsume("user1", EndpointCategory.GENERAL);
        }
        ConsumptionProbe probeUser1 = manager.tryConsume("user1", EndpointCategory.GENERAL);
        assertFalse(probeUser1.isConsumed());

        ConsumptionProbe probeUser2 = manager.tryConsume("user2", EndpointCategory.GENERAL);
        assertTrue(probeUser2.isConsumed());
    }

    @Test
    void shouldTrackDifferentCategoriesIndependently() {
        for (int i = 0; i < 5; i++) {
            manager.tryConsume("user1", EndpointCategory.GENERAL);
        }
        ConsumptionProbe generalProbe = manager.tryConsume("user1", EndpointCategory.GENERAL);
        assertFalse(generalProbe.isConsumed());

        ConsumptionProbe postProbe = manager.tryConsume("user1", EndpointCategory.POST);
        assertTrue(postProbe.isConsumed());
    }

    @Test
    void shouldEnforcePostCategoryLimit() {
        for (int i = 0; i < 3; i++) {
            ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.POST);
            assertTrue(probe.isConsumed());
        }
        ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.POST);
        assertFalse(probe.isConsumed());
    }

    @Test
    void shouldEnforceAiCategoryLimit() {
        for (int i = 0; i < 2; i++) {
            ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.AI);
            assertTrue(probe.isConsumed());
        }
        ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.AI);
        assertFalse(probe.isConsumed());
    }

    @Test
    void shouldProvideRemainingTokenCount() {
        ConsumptionProbe probe = manager.tryConsume("user1", EndpointCategory.GENERAL);
        assertTrue(probe.isConsumed());
        assertTrue(probe.getRemainingTokens() == 4);
    }
}
