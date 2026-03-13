package com.akazukin.sdk.pinterest;

public record PinterestConfig(
    String appId,
    String appSecret,
    String redirectUri
) {
    public PinterestConfig {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("appId required");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("appSecret required");
        }
    }
}
