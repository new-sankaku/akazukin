package com.akazukin.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ApprovalDecision(
    ApprovalAction action,
    String comment,
    UUID decidedBy,
    Instant decidedAt
) {
}
