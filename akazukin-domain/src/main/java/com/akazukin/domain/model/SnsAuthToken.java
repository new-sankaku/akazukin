package com.akazukin.domain.model;

import java.time.Instant;

public record SnsAuthToken(
    String accessToken,
    String refreshToken,
    Instant expiresAt,
    String scope
) {
}
