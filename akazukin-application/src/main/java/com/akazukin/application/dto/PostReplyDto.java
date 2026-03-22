package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PostReplyDto(
    UUID id,
    String userName,
    String platform,
    String content,
    Instant createdAt
) {}
