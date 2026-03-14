package com.akazukin.sdk.ollama;

import java.time.Duration;

public record OllamaConfig(
    String baseUrl,
    String defaultModel,
    Duration timeout
) {
    public OllamaConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (defaultModel == null || defaultModel.isBlank()) {
            throw new IllegalArgumentException("defaultModel must not be blank");
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }
}
