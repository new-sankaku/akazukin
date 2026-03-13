package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.infrastructure.persistence.entity.PostTargetEntity;
import com.akazukin.infrastructure.persistence.mapper.PostTargetMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostTargetRepositoryImpl implements PostTargetRepository, PanacheRepository<PostTargetEntity> {

    @Override
    public Optional<PostTarget> findById(UUID id) {
        return find("id", id).firstResultOptional().map(PostTargetMapper::toDomain);
    }

    @Override
    public List<PostTarget> findByPostId(UUID postId) {
        return find("postId", postId)
                .list()
                .stream()
                .map(PostTargetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostTarget save(PostTarget target) {
        PostTargetEntity entity = PostTargetMapper.toEntity(target);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return PostTargetMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteByPostId(UUID postId) {
        delete("postId", postId);
    }

    @Override
    public List<PostTarget> findByPostIds(List<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return find("postId in ?1", postIds)
                .list()
                .stream()
                .map(PostTargetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, PostStatus status, String errorMessage) {
        update("status = ?1, errorMessage = ?2 where id = ?3", status.name(), errorMessage, id);
    }
}
