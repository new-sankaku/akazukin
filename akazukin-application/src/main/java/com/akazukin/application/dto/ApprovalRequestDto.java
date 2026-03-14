package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestDto(
    UUID id,
    UUID postId,
    String postContent,
    String requesterName,
    String status,
    String comment,
    Instant requestedAt,
    Instant decidedAt
) {}
