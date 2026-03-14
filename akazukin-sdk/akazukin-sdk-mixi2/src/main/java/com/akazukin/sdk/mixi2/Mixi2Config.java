package com.akazukin.sdk.mixi2;

public record Mixi2Config(
    String clientId,
    String clientSecret,
    String redirectUri
) {
    public Mixi2Config {
        clientId = clientId != null ? clientId : "";
        clientSecret = clientSecret != null ? clientSecret : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
