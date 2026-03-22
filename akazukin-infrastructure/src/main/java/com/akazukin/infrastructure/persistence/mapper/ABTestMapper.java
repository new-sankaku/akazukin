package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.model.ABTestStatus;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.ABTestEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ABTestMapper {

    private ABTestMapper() {
    }

    public static ABTest toDomain(ABTestEntity entity) {
        return new ABTest(
                entity.id,
                entity.userId,
                entity.name,
                entity.variantA,
                entity.variantB,
                entity.variantC,
                ABTestStatus.valueOf(entity.status),
                entity.startedAt,
                entity.completedAt,
                entity.winnerVariant,
                parsePlatforms(entity.platforms),
                entity.createdAt
        );
    }

    public static ABTestEntity toEntity(ABTest domain) {
        ABTestEntity entity = new ABTestEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.name = domain.getName();
        entity.variantA = domain.getVariantA();
        entity.variantB = domain.getVariantB();
        entity.variantC = domain.getVariantC();
        entity.status = domain.getStatus().name();
        entity.startedAt = domain.getStartedAt();
        entity.completedAt = domain.getCompletedAt();
        entity.winnerVariant = domain.getWinnerVariant();
        entity.platforms = serializePlatforms(domain.getPlatforms());
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }

    private static List<SnsPlatform> parsePlatforms(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
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
                .collect(Collectors.joining(","));
    }
}
