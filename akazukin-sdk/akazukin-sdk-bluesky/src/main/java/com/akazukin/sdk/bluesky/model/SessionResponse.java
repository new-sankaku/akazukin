package com.akazukin.sdk.bluesky.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionResponse(
    @JsonProperty("did") String did,
    @JsonProperty("handle") String handle,
    @JsonProperty("accessJwt") String accessJwt,
    @JsonProperty("refreshJwt") String refreshJwt
) {
}
