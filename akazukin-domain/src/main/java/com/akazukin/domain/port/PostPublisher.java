package com.akazukin.domain.port;

import java.time.Instant;
import java.util.UUID;

public interface PostPublisher {

    void publishForProcessing(UUID postId);

    void schedulePost(UUID postId, Instant scheduledAt);

    void cancelScheduledPost(UUID postId);
}
