package com.akazukin.infrastructure.news;

import com.akazukin.domain.model.NewsSource;
import com.akazukin.domain.port.NewsSourceRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NewsSourceRepositoryImpl implements NewsSourceRepository, PanacheRepository<NewsSourceEntity> {

    @Override
    public Optional<NewsSource> findById(UUID id) {
        return find("id", id)
                .firstResultOptional()
                .map(NewsSourceMapper::toDomain);
    }

    @Override
    public List<NewsSource> findByUserId(UUID userId) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId)
                .list()
                .stream()
                .map(NewsSourceMapper::toDomain)
                .toList();
    }

    @Override
    public List<NewsSource> findActiveByUserId(UUID userId) {
        return find("userId = ?1 AND isActive = true ORDER BY createdAt DESC", userId)
                .list()
                .stream()
                .map(NewsSourceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public NewsSource save(NewsSource newsSource) {
        NewsSourceEntity entity = NewsSourceMapper.toEntity(newsSource);
        persist(entity);
        return NewsSourceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
