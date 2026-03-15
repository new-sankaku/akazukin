package com.akazukin.sdk.twitter;

public record TwitterConfig(
    String clientId,
    String clientSecret,
    String redirectUri
) {
    public TwitterConfig {
        clientId = clientId != null ? clientId : "";
        clientSecret = clientSecret != null ? clientSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
