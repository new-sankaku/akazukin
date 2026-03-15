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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostTargetRepositoryImpl implements PostTargetRepository, PanacheRepository<PostTargetEntity> {

    private static final Logger LOG = Logger.getLogger(PostTargetRepositoryImpl.class.getName());

    @Override
    public Optional<PostTarget> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(PostTargetMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public List<PostTarget> findByPostId(UUID postId) {
        long perfStart = System.nanoTime();
        try {
            return find("postId", postId)
                    .list()
                    .stream()
                    .map(PostTargetMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findByPostId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findByPostId", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public PostTarget save(PostTarget target) {
        long perfStart = System.nanoTime();
        try {
            PostTargetEntity entity = PostTargetMapper.toEntity(target);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return PostTargetMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void deleteByPostId(UUID postId) {
        long perfStart = System.nanoTime();
        try {
            delete("postId", postId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.deleteByPostId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.deleteByPostId", perfMs});
            }
        }
    }

    @Override
    public List<PostTarget> findByPostIds(List<UUID> postIds) {
        long perfStart = System.nanoTime();
        try {
            if (postIds == null || postIds.isEmpty()) {
                return List.of();
            }
            return find("postId in ?1", postIds)
                    .list()
                    .stream()
                    .map(PostTargetMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findByPostIds", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.findByPostIds", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, PostStatus status, String errorMessage) {
        long perfStart = System.nanoTime();
        try {
            update("status = ?1, errorMessage = ?2 where id = ?3", status.name(), errorMessage, id);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.updateStatus", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostTargetRepositoryImpl.updateStatus", perfMs});
            }
        }
    }
}
