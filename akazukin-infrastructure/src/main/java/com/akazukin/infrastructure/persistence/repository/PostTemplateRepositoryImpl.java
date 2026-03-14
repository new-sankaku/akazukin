package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.port.PostTemplateRepository;
import com.akazukin.infrastructure.persistence.entity.PostTemplateEntity;
import com.akazukin.infrastructure.persistence.mapper.PostTemplateMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostTemplateRepositoryImpl implements PostTemplateRepository, PanacheRepository<PostTemplateEntity> {

    @Override
    public Optional<PostTemplate> findById(UUID id) {
        return find("id", id).firstResultOptional().map(PostTemplateMapper::toDomain);
    }

    @Override
    public List<PostTemplate> findByUserId(UUID userId) {
        return find("userId", Sort.by("createdAt").descending(), userId)
                .list()
                .stream()
                .map(PostTemplateMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostTemplate save(PostTemplate postTemplate) {
        PostTemplateEntity entity = PostTemplateMapper.toEntity(postTemplate);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return PostTemplateMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }

    @Override
    @Transactional
    public void incrementUsageCount(UUID id) {
        update("usageCount = usageCount + 1 WHERE id = ?1", id);
    }
}
