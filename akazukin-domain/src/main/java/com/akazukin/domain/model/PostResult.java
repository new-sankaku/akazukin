package com.akazukin.domain.model;

import java.time.Instant;

public record PostResult(
    String platformPostId,
    String platformUrl,
    Instant publishedAt
) {
}
