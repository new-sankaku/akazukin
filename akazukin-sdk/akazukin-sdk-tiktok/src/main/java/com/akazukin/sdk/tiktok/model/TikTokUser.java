package com.akazukin.sdk.tiktok.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TikTokUser(
    @JsonProperty("open_id") String openId,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("avatar_url") String avatarUrl,
    @JsonProperty("username") String username,
    @JsonProperty("follower_count") int followerCount,
    @JsonProperty("following_count") int followingCount,
    @JsonProperty("likes_count") int likesCount,
    @JsonProperty("video_count") int videoCount,
    @JsonProperty("is_verified") boolean isVerified
) {
}
