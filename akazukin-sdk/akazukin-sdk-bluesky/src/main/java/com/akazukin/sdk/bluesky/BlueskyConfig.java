package com.akazukin.sdk.bluesky;

public record BlueskyConfig(
    String handle,
    String appPassword
) {
    public BlueskyConfig {
        if (handle == null || handle.isBlank()) {
            throw new IllegalArgumentException("handle must not be blank");
        }
        if (appPassword == null || appPassword.isBlank()) {
            throw new IllegalArgumentException("appPassword must not be blank");
        }
    }
}
