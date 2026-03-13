package com.akazukin.sdk.reddit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RedditUser(
    @JsonProperty("name") String name,
    @JsonProperty("id") String id,
    @JsonProperty("link_karma") int linkKarma,
    @JsonProperty("comment_karma") int commentKarma,
    @JsonProperty("icon_img") String iconImg
) {
}
