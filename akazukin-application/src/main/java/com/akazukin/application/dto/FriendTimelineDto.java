package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record FriendTimelineDto(
    UUID interactionId,
    Instant date,
    String description,
    String detail,
    boolean highlight
) {}
