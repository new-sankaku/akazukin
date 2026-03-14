package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.CalendarEntry;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.CalendarEntryEntity;

import java.util.Arrays;
import java.util.List;

public final class CalendarEntryMapper {

    private CalendarEntryMapper() {
    }

    public static CalendarEntry toDomain(CalendarEntryEntity entity) {
        List<SnsPlatform> platforms = parsePlatforms(entity.platforms);

        return new CalendarEntry(
                entity.id,
                entity.userId,
                entity.postId,
                entity.title,
                entity.description,
                entity.scheduledAt,
                platforms,
                entity.color,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static CalendarEntryEntity toEntity(CalendarEntry domain) {
        CalendarEntryEntity entity = new CalendarEntryEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.postId = domain.getPostId();
        entity.title = domain.getTitle();
        entity.description = domain.getDescription();
        entity.scheduledAt = domain.getScheduledAt();
        entity.platforms = serializePlatforms(domain.getPlatforms());
        entity.color = domain.getColor();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    private static List<SnsPlatform> parsePlatforms(String platformsStr) {
        if (platformsStr == null || platformsStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(platformsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SnsPlatform::valueOf)
                .toList();
    }

    private static String serializePlatforms(List<SnsPlatform> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return null;
        }
        return platforms.stream()
                .map(SnsPlatform::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}
