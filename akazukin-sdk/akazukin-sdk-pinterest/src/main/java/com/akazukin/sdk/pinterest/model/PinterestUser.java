package com.akazukin.sdk.pinterest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PinterestUser(
    @JsonProperty("username") String username,
    @JsonProperty("account_type") String accountType,
    @JsonProperty("profile_image") String profileImage,
    @JsonProperty("follower_count") int followerCount,
    @JsonProperty("pin_count") int pinCount
) {
}
