package com.akazukin.sdk.pinterest;

public record PinterestConfig(
    String appId,
    String appSecret,
    String redirectUri
) {
    public PinterestConfig {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("appId must not be null or blank");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("appSecret must not be null or blank");
        }
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
