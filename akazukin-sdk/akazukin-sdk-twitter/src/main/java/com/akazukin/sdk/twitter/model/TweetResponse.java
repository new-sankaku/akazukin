package com.akazukin.sdk.twitter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TweetResponse(
    @JsonProperty("id") String id,
    @JsonProperty("text") String text
) {
}
