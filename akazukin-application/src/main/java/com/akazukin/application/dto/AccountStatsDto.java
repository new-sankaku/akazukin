package com.akazukin.application.dto;

import java.time.Instant;

public record AccountStatsDto(
    String platform,
    String accountIdentifier,
    int followerCount,
    int followingCount,
    int postCount,
    Instant fetchedAt
) {}
