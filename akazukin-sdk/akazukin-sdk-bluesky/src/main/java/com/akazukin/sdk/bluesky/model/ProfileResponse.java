package com.akazukin.sdk.bluesky.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileResponse(
    @JsonProperty("did") String did,
    @JsonProperty("handle") String handle,
    @JsonProperty("displayName") String displayName,
    @JsonProperty("avatar") String avatar,
    @JsonProperty("followersCount") int followersCount,
    @JsonProperty("followsCount") int followsCount,
    @JsonProperty("postsCount") int postsCount
) {
}
