package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationRepository;
import com.akazukin.infrastructure.persistence.entity.NotificationEntity;
import com.akazukin.infrastructure.persistence.mapper.NotificationMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationRepositoryImpl implements NotificationRepository, PanacheRepository<NotificationEntity> {

    @Override
    public Optional<Notification> findById(UUID id) {
        return find("id", id).firstResultOptional().map(NotificationMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UUID userId, int offset, int limit) {
        return find("userId", Sort.by("createdAt").descending(), userId)
                .page(offset / limit, limit)
                .list()
                .stream()
                .map(NotificationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findUnreadByUserId(UUID userId) {
        return find("userId = ?1 AND isRead = false", userId)
                .list()
                .stream()
                .map(NotificationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationMapper.toEntity(notification);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return NotificationMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void markAsRead(UUID id) {
        update("isRead = true WHERE id = ?1", id);
    }

    @Override
    @Transactional
    public void markAllAsReadByUserId(UUID userId) {
        update("isRead = true WHERE userId = ?1 AND isRead = false", userId);
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return count("userId = ?1 AND isRead = false", userId);
    }
}
