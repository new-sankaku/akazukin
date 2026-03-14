package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.PostTemplateEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PostTemplateMapper {
    private static final String SEPARATOR = "\n";

    private PostTemplateMapper() {
    }

    public static PostTemplate toDomain(PostTemplateEntity entity) {
        List<String> placeholders = parseList(entity.placeholders);
        List<SnsPlatform> platforms = parsePlatforms(entity.platforms);
        return new PostTemplate(
                entity.id,
                entity.userId,
                entity.name,
                entity.content,
                placeholders,
                platforms,
                entity.category,
                entity.usageCount,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static PostTemplateEntity toEntity(PostTemplate domain) {
        PostTemplateEntity entity = new PostTemplateEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.name = domain.getName();
        entity.content = domain.getContent();
        entity.placeholders = serializeList(domain.getPlaceholders());
        entity.platforms = serializePlatforms(domain.getPlatforms());
        entity.category = domain.getCategory();
        entity.usageCount = domain.getUsageCount();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(SEPARATOR))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static String serializeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(SEPARATOR, values);
    }

    private static List<SnsPlatform> parsePlatforms(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(SEPARATOR))
                .filter(s -> !s.isBlank())
                .map(SnsPlatform::valueOf)
                .collect(Collectors.toList());
    }

    private static String serializePlatforms(List<SnsPlatform> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return "";
        }
        return platforms.stream()
                .map(SnsPlatform::name)
                .collect(Collectors.joining(SEPARATOR));
    }
}
