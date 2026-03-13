package com.akazukin.sdk.pinterest;

public record PinterestConfig(
    String appId,
    String appSecret,
    String accessToken
) {
    public PinterestConfig {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("appId must not be blank");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("appSecret must not be blank");
        }
    }
}
