package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.Interaction;
import com.akazukin.domain.model.InteractionType;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.InteractionEntity;

public final class InteractionMapper {

    private InteractionMapper() {
    }

    public static Interaction toDomain(InteractionEntity entity) {
        return new Interaction(
                entity.id,
                entity.userId,
                entity.snsAccountId,
                SnsPlatform.valueOf(entity.platform),
                InteractionType.valueOf(entity.interactionType),
                entity.targetPostId,
                entity.targetUserId,
                entity.content,
                entity.createdAt
        );
    }

    public static InteractionEntity toEntity(Interaction domain) {
        InteractionEntity entity = new InteractionEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.snsAccountId = domain.getSnsAccountId();
        entity.platform = domain.getPlatform().name();
        entity.interactionType = domain.getInteractionType().name();
        entity.targetPostId = domain.getTargetPostId();
        entity.targetUserId = domain.getTargetUserId();
        entity.content = domain.getContent();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
