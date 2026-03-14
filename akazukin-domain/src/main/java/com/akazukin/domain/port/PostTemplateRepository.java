package com.akazukin.domain.port;

import com.akazukin.domain.model.PostTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostTemplateRepository {

    Optional<PostTemplate> findById(UUID id);

    List<PostTemplate> findByUserId(UUID userId);

    PostTemplate save(PostTemplate postTemplate);

    void deleteById(UUID id);

    void incrementUsageCount(UUID id);
}
