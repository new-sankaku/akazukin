package com.akazukin.sdk.bluesky;

public record BlueskyConfig(String serviceUrl) {

    public BlueskyConfig {
        if (serviceUrl == null || serviceUrl.isBlank()) {
            throw new IllegalArgumentException("serviceUrl required");
        }
    }

    public static BlueskyConfig defaultConfig() {
        return new BlueskyConfig("https://bsky.social");
    }
}
