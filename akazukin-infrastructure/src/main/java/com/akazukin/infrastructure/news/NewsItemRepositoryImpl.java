package com.akazukin.infrastructure.news;

import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.port.NewsItemRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NewsItemRepositoryImpl implements NewsItemRepository, PanacheRepository<NewsItemEntity> {

    @Override
    public Optional<NewsItem> findById(UUID id) {
        return find("id", id)
                .firstResultOptional()
                .map(NewsItemMapper::toDomain);
    }

    @Override
    public List<NewsItem> findBySourceId(UUID sourceId, int offset, int limit) {
        int effectiveLimit = Math.max(limit, 1);
        return find("sourceId = ?1 ORDER BY fetchedAt DESC", sourceId)
                .page(offset / effectiveLimit, effectiveLimit)
                .list()
                .stream()
                .map(NewsItemMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public NewsItem save(NewsItem newsItem) {
        NewsItemEntity entity = NewsItemMapper.toEntity(newsItem);
        persist(entity);
        return NewsItemMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteBySourceId(UUID sourceId) {
        delete("sourceId", sourceId);
    }
}
