package com.akazukin.domain.port;

import com.akazukin.domain.model.NewsSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsSourceRepository {

    Optional<NewsSource> findById(UUID id);

    List<NewsSource> findByUserId(UUID userId);

    List<NewsSource> findActiveByUserId(UUID userId);

    NewsSource save(NewsSource newsSource);

    void deleteById(UUID id);
}
