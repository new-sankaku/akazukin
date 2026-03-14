package com.akazukin.sdk.mixi2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Mixi2PostResponse(
    @JsonProperty("id") String id,
    @JsonProperty("text") String text,
    @JsonProperty("created_at") String createdAt
) {
}
