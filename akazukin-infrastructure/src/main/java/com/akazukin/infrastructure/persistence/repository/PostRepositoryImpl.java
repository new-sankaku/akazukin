package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.infrastructure.persistence.entity.PostEntity;
import com.akazukin.infrastructure.persistence.mapper.PostMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostRepositoryImpl implements PostRepository, PanacheRepository<PostEntity> {

    private static final Logger LOG = Logger.getLogger(PostRepositoryImpl.class.getName());

    @Override
    public Optional<Post> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(PostMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public List<Post> findByUserId(UUID userId, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("userId", Sort.by("createdAt").descending(), userId)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(PostMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findByUserId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findByUserId", perfMs});
            }
        }
    }

    @Override
    public List<Post> findScheduledBefore(Instant before) {
        long perfStart = System.nanoTime();
        try {
            return find("status = ?1 and scheduledAt <= ?2", PostStatus.SCHEDULED.name(), before)
                    .list()
                    .stream()
                    .map(PostMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findScheduledBefore", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.findScheduledBefore", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public Post save(Post post) {
        long perfStart = System.nanoTime();
        try {
            PostEntity entity = PostMapper.toEntity(post);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return PostMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            delete("id", id);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.deleteById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.deleteById", perfMs});
            }
        }
    }

    @Override
    public long countByUserId(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return count("userId", userId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserId", perfMs});
            }
        }
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, PostStatus status) {
        long perfStart = System.nanoTime();
        try {
            return count("userId = ?1 and status = ?2", userId, status.name());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserIdAndStatus", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserIdAndStatus", perfMs});
            }
        }
    }

    @Override
    public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<Object[]> results = getEntityManager()
                    .createQuery(
                            "SELECT pt.platform, COUNT(pt) FROM PostTargetEntity pt"
                                    + " JOIN PostEntity p ON pt.postId = p.id"
                                    + " WHERE p.userId = :userId GROUP BY pt.platform",
                            Object[].class)
                    .setParameter("userId", userId)
                    .getResultList();
            Map<SnsPlatform, Long> counts = new EnumMap<>(SnsPlatform.class);
            for (Object[] row : results) {
                counts.put(SnsPlatform.valueOf((String) row[0]), (Long) row[1]);
            }
            return counts;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserIdGroupByPlatform", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostRepositoryImpl.countByUserIdGroupByPlatform", perfMs});
            }
        }
    }
}
