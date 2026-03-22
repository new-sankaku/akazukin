package com.akazukin.sdk.twitter;

public record TwitterConfig(
    String clientId,
    String clientSecret,
    String redirectUri
) {
    public TwitterConfig {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret must not be null or blank");
        }
        if (redirectUri == null) {
            redirectUri = "";
        }
    }
}
