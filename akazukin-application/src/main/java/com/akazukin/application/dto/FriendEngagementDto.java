package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FriendEngagementDto(
    UUID friendId,
    String platform,
    String targetIdentifier,
    String displayName,
    String notes,
    int rank,
    int daysSinceLastInteraction,
    String priorityLevel,
    int relationshipScore,
    String scoreTrend,
    List<Integer> scoreHistory,
    EngagementMetricsDto metrics,
    String analystNote,
    Instant lastInteractionAt
) {}
