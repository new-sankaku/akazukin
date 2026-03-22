package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record FriendTimelineResponseDto(
    UUID friendId,
    String displayName,
    String platform,
    String targetIdentifier,
    String notes,
    int relationshipScore,
    String scoreTrend,
    List<Integer> scoreHistory,
    EngagementMetricsDto metrics,
    String analystNote,
    List<FriendTimelineDto> timeline
) {}
