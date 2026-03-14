package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.FriendTarget;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.FriendTargetEntity;

public final class FriendTargetMapper {

    private FriendTargetMapper() {
    }

    public static FriendTarget toDomain(FriendTargetEntity entity) {
        return new FriendTarget(
                entity.id,
                entity.userId,
                SnsPlatform.valueOf(entity.platform),
                entity.targetIdentifier,
                entity.displayName,
                entity.notes,
                entity.createdAt
        );
    }

    public static FriendTargetEntity toEntity(FriendTarget domain) {
        FriendTargetEntity entity = new FriendTargetEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.platform = domain.getPlatform().name();
        entity.targetIdentifier = domain.getTargetIdentifier();
        entity.displayName = domain.getDisplayName();
        entity.notes = domain.getNotes();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
