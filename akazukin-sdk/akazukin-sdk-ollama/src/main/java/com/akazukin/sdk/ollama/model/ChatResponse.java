package com.akazukin.sdk.ollama.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResponse(
    @JsonProperty("message") ChatMessage message,
    @JsonProperty("model") String model,
    @JsonProperty("total_duration") long totalDuration,
    @JsonProperty("eval_count") int evalCount
) {
}
