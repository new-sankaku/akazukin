package com.akazukin.sdk.pinterest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PinResponse(
    @JsonProperty("id") String id,
    @JsonProperty("title") String title,
    @JsonProperty("board_id") String boardId
) {
}
