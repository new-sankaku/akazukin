package com.akazukin.sdk.ollama.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelInfo(
    @JsonProperty("name") String name,
    @JsonProperty("size") long size,
    @JsonProperty("modified_at") String modifiedAt
) {
}
