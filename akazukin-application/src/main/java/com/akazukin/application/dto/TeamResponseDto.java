package com.akazukin.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamResponseDto(
    UUID id,
    String name,
    UUID ownerUserId,
    List<TeamMemberDto> members,
    Instant createdAt
) {}
