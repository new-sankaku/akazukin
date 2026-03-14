package com.akazukin.sdk.note.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteUser(
    @JsonProperty("id") String id,
    @JsonProperty("urlname") String urlname,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("profile_image_url") String profileImageUrl
) {
}
