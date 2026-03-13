package com.akazukin.domain.model;

import java.time.Instant;

public record SnsPostStats(
    String platformPostId,
    SnsPlatform platform,
    int likeCount,
    int replyCount,
    int repostCount,
    int viewCount,
    Instant fetchedAt
) {}
