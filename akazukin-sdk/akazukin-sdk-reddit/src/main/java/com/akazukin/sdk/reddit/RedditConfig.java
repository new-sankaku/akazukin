package com.akazukin.sdk.reddit;

public record RedditConfig(
    String clientId,
    String clientSecret,
    String userAgent
) {
    public RedditConfig {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret must not be blank");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent must not be blank");
        }
    }
}
