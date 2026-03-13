package com.akazukin.sdk.bluesky.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostResponse(
    @JsonProperty("uri") String uri,
    @JsonProperty("cid") String cid
) {
}
