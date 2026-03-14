package com.akazukin.sdk.ollama.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatMessage(
    @JsonProperty("role") String role,
    @JsonProperty("content") String content
) {
}
