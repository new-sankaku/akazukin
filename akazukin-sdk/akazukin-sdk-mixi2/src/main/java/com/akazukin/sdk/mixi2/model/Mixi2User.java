package com.akazukin.sdk.mixi2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Mixi2User(
    @JsonProperty("id") String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("profile_image_url") String profileImageUrl
) {
}
