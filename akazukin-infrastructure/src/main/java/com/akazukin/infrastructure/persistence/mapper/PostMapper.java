package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.infrastructure.persistence.entity.PostEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PostMapper {
    private static final String MEDIA_URL_SEPARATOR = "\n";

    private PostMapper() {}

    public static Post toDomain(PostEntity entity) {
        List<String> mediaUrls = parseMediaUrls(entity.mediaUrls);
        return new Post(
                entity.id,
                entity.userId,
                entity.content,
                mediaUrls,
                PostStatus.valueOf(entity.status),
                entity.scheduledAt,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static PostEntity toEntity(Post domain) {
        PostEntity entity = new PostEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.content = domain.getContent();
        entity.mediaUrls = serializeMediaUrls(domain.getMediaUrls());
        entity.status = domain.getStatus().name();
        entity.scheduledAt = domain.getScheduledAt();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    private static List<String> parseMediaUrls(String mediaUrls) {
        if (mediaUrls == null || mediaUrls.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(mediaUrls.split(MEDIA_URL_SEPARATOR))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static String serializeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return "";
        }
        return String.join(MEDIA_URL_SEPARATOR, mediaUrls);
    }
}
