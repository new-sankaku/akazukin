package com.akazukin.sdk.note.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotePostResponse(
    @JsonProperty("id") String id,
    @JsonProperty("key") String key,
    @JsonProperty("published_at") String publishedAt
) {
}
