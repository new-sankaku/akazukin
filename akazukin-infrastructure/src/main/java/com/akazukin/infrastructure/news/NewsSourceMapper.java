package com.akazukin.infrastructure.news;

import com.akazukin.domain.model.NewsSource;

public final class NewsSourceMapper {

    private NewsSourceMapper() {
    }

    public static NewsSource toDomain(NewsSourceEntity entity) {
        return new NewsSource(
                entity.id,
                entity.userId,
                entity.name,
                entity.url,
                entity.sourceType,
                entity.isActive,
                entity.createdAt
        );
    }

    public static NewsSourceEntity toEntity(NewsSource domain) {
        NewsSourceEntity entity = new NewsSourceEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.name = domain.getName();
        entity.url = domain.getUrl();
        entity.sourceType = domain.getSourceType();
        entity.isActive = domain.isActive();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
