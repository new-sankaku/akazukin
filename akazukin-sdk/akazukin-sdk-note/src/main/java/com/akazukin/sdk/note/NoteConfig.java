package com.akazukin.sdk.note;

public record NoteConfig(
    String apiKey,
    String redirectUri
) {
    public NoteConfig {
        apiKey = apiKey != null ? apiKey : "";
        redirectUri = redirectUri != null ? redirectUri : "";
    }
}
