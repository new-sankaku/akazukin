package com.akazukin.domain.model;

import java.time.Instant;

public record AccountStats(
    SnsPlatform platform,
    String accountIdentifier,
    int followerCount,
    int followingCount,
    int postCount,
    Instant fetchedAt
) {}
