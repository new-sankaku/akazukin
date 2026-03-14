package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ImpressionSnapshotDto(
    UUID id,
    UUID snsAccountId,
    String platform,
    int followersCount,
    int followingCount,
    int postCount,
    long impressionsCount,
    double engagementRate,
    Instant snapshotAt
) {}
