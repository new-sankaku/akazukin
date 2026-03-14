package com.akazukin.sdk.tiktok.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublishResponse(
    @JsonProperty("publish_id") String publishId,
    @JsonProperty("upload_url") String uploadUrl
) {
}
