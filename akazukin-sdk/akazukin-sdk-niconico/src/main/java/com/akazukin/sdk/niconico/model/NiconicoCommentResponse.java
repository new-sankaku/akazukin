package com.akazukin.sdk.niconico.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NiconicoCommentResponse(
    @JsonProperty("comment_id") String commentId,
    @JsonProperty("posted_at") String postedAt
) {
}
