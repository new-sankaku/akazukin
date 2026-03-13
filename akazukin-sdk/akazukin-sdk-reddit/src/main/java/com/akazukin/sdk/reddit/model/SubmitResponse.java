package com.akazukin.sdk.reddit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitResponse(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("url") String url
) {
}
