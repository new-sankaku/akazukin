package com.akazukin.sdk.tiktok.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublishStatusResponse(
    @JsonProperty("status") String status,
    @JsonProperty("publish_id") String publishId
) {

    public boolean isComplete() {
        return "PUBLISH_COMPLETE".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isProcessing() {
        return status != null && status.startsWith("PROCESSING");
    }
}
