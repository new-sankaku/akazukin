package com.akazukin.sdk.tiktok;

public record TikTokConfig(
    String clientKey,
    String clientSecret,
    String redirectUri
) {
    public TikTokConfig {
        clientKey = clientKey != null ? clientKey : "";
        clientSecret = clientSecret != null ? clientSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
