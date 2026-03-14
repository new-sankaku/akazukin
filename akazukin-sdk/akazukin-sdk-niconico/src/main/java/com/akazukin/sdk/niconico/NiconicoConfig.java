package com.akazukin.sdk.niconico;

public record NiconicoConfig(
    String apiKey,
    String redirectUri
) {
    public NiconicoConfig {
        apiKey = apiKey != null ? apiKey : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
