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
import java.util.stream.Collectors;

@ApplicationScoped
public class PostRepositoryImpl implements PostRepository, PanacheRepository<PostEntity> {

    @Override
    public Optional<Post> findById(UUID id) {
        return find("id", id).firstResultOptional().map(PostMapper::toDomain);
    }

    @Override
    public List<Post> findByUserId(UUID userId, int offset, int limit) {
        return find("userId", Sort.by("createdAt").descending(), userId)
                .page(offset / limit, limit)
                .list()
                .stream()
                .map(PostMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> findScheduledBefore(Instant before) {
        return find("status = ?1 and scheduledAt <= ?2", PostStatus.SCHEDULED.name(), before)
                .list()
                .stream()
                .map(PostMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Post save(Post post) {
        PostEntity entity = PostMapper.toEntity(post);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return PostMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }

    @Override
    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, PostStatus status) {
        return count("userId = ?1 and status = ?2", userId, status.name());
    }

    @Override
    public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
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
            counts.put((SnsPlatform) row[0], (Long) row[1]);
        }
        return counts;
    }
}
