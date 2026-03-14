package com.akazukin.domain.port;

import com.akazukin.domain.model.NewsItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsItemRepository {

    Optional<NewsItem> findById(UUID id);

    List<NewsItem> findBySourceId(UUID sourceId, int offset, int limit);

    NewsItem save(NewsItem newsItem);

    void deleteBySourceId(UUID sourceId);
}
