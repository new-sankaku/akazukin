package com.akazukin.infrastructure.news;

import com.akazukin.domain.model.NewsItem;

public final class NewsItemMapper {

    private NewsItemMapper() {
    }

    public static NewsItem toDomain(NewsItemEntity entity) {
        return new NewsItem(
                entity.id,
                entity.sourceId,
                entity.title,
                entity.url,
                entity.summary,
                entity.publishedAt,
                entity.fetchedAt
        );
    }

    public static NewsItemEntity toEntity(NewsItem domain) {
        NewsItemEntity entity = new NewsItemEntity();
        entity.id = domain.getId();
        entity.sourceId = domain.getSourceId();
        entity.title = domain.getTitle();
        entity.url = domain.getUrl();
        entity.summary = domain.getSummary();
        entity.publishedAt = domain.getPublishedAt();
        entity.fetchedAt = domain.getFetchedAt();
        return entity;
    }
}
