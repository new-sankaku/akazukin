package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberDto(
    UUID userId,
    String username,
    String role,
    Instant joinedAt
) {}
