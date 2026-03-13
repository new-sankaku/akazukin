package com.akazukin.sdk.twitter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwitterUser(
    @JsonProperty("id") String id,
    @JsonProperty("username") String username,
    @JsonProperty("name") String name,
    @JsonProperty("profile_image_url") String profileImageUrl,
    @JsonProperty("followers_count") int followersCount,
    @JsonProperty("following_count") int followingCount
) {
}
