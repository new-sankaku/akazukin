package com.akazukin.application.dto;

import java.util.UUID;

public record BubbleItemDto(
    UUID friendId,
    String displayName,
    String platform,
    int relationshipScore,
    int engagementFrequency,
    String status
) {}
