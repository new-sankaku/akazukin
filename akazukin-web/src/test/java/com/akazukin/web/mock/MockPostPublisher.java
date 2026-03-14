package com.akazukin.web.mock;

import com.akazukin.domain.port.PostPublisher;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

@Mock
@ApplicationScoped
public class MockPostPublisher implements PostPublisher {

    @Override
    public void publishForProcessing(UUID postId) {
        // No-op for tests
    }

    @Override
    public void schedulePost(UUID postId, Instant scheduledAt) {
        // No-op for tests
    }

    @Override
    public void cancelScheduledPost(UUID postId) {
        // No-op for tests
    }
}
