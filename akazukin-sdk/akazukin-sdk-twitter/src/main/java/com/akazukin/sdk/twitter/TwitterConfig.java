package com.akazukin.sdk.twitter;

public record TwitterConfig(
    String apiKey,
    String apiSecret,
    String accessToken,
    String accessTokenSecret
) {
    public TwitterConfig {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalArgumentException("apiSecret must not be blank");
        }
    }
}
