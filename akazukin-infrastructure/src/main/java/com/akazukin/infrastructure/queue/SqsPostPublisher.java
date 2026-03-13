package com.akazukin.infrastructure.queue;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class SqsPostPublisher {
    public void publishPostMessage(UUID postId) {
        // TODO: Implement SQS publishing
        throw new UnsupportedOperationException("SQS publishing not yet implemented");
    }
}
