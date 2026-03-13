package com.akazukin.sdk.reddit;

public record RedditConfig(
    String clientId,
    String clientSecret,
    String redirectUri,
    String userAgent
) {
    public RedditConfig {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId required");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent required");
        }
    }
}
