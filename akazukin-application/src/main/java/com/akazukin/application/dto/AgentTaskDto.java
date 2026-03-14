package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AgentTaskDto(
    UUID id,
    String agentType,
    String input,
    String output,
    String status,
    UUID parentTaskId,
    Instant createdAt,
    Instant completedAt
) {
}
