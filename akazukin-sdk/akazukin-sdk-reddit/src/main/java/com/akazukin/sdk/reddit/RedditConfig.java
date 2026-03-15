package com.akazukin.sdk.reddit;

public record RedditConfig(
    String clientId,
    String clientSecret,
    String redirectUri,
    String userAgent
) {
    public RedditConfig {
        clientId = clientId != null ? clientId : "";
        clientSecret = clientSecret != null ? clientSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
        userAgent = (userAgent == null || userAgent.isBlank()) ? "akazukin/1.0" : userAgent;
    }
}
