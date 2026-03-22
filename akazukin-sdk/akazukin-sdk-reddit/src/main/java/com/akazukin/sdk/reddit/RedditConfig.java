package com.akazukin.sdk.reddit;

public record RedditConfig(
    String clientId,
    String clientSecret,
    String redirectUri,
    String userAgent
) {
    public RedditConfig {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }
        clientSecret = clientSecret != null ? clientSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent must not be null or blank");
        }
    }
}
