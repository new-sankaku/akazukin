package com.akazukin.sdk.ollama.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateResponse(
    @JsonProperty("response") String response,
    @JsonProperty("model") String model,
    @JsonProperty("total_duration") long totalDuration,
    @JsonProperty("eval_count") int evalCount
) {
}
