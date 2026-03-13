package com.akazukin.domain.model;

import java.util.List;

public record PostRequest(
    String content,
    List<String> mediaUrls
) {
    public PostRequest {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post content must not be blank");
        }
        mediaUrls = mediaUrls != null ? List.copyOf(mediaUrls) : List.of();
    }
}
