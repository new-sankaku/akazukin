package com.akazukin.sdk.twitter;

public record TwitterConfig(
    String clientId,
    String clientSecret,
    String redirectUri
) {
    public TwitterConfig {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId required");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret required");
        }
    }
}
