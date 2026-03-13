package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.PostTargetEntity;

public final class PostTargetMapper {
    private PostTargetMapper() {}

    public static PostTarget toDomain(PostTargetEntity entity) {
        return new PostTarget(
                entity.id,
                entity.postId,
                entity.snsAccountId,
                SnsPlatform.valueOf(entity.platform),
                entity.platformPostId,
                PostStatus.valueOf(entity.status),
                entity.errorMessage,
                entity.publishedAt,
                entity.createdAt
        );
    }

    public static PostTargetEntity toEntity(PostTarget domain) {
        PostTargetEntity entity = new PostTargetEntity();
        entity.id = domain.getId();
        entity.postId = domain.getPostId();
        entity.snsAccountId = domain.getSnsAccountId();
        entity.platform = domain.getPlatform().name();
        entity.platformPostId = domain.getPlatformPostId();
        entity.status = domain.getStatus().name();
        entity.errorMessage = domain.getErrorMessage();
        entity.publishedAt = domain.getPublishedAt();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
