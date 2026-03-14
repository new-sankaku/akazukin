package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.model.ABTestStatus;
import com.akazukin.infrastructure.persistence.entity.ABTestEntity;

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
                ABTestStatus.valueOf(entity.status),
                entity.startedAt,
                entity.completedAt,
                entity.winnerVariant,
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
        entity.status = domain.getStatus().name();
        entity.startedAt = domain.getStartedAt();
        entity.completedAt = domain.getCompletedAt();
        entity.winnerVariant = domain.getWinnerVariant();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
