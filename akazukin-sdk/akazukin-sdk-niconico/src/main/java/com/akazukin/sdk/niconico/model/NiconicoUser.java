package com.akazukin.sdk.niconico.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NiconicoUser(
    @JsonProperty("id") String id,
    @JsonProperty("nickname") String nickname,
    @JsonProperty("icon_url") String iconUrl
) {
}
