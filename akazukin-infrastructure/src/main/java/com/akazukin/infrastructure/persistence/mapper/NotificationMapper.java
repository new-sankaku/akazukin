package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.model.NotificationType;
import com.akazukin.infrastructure.persistence.entity.NotificationEntity;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.id,
                entity.userId,
                NotificationType.valueOf(entity.type),
                entity.title,
                entity.body,
                entity.relatedEntityId,
                entity.isRead,
                entity.createdAt
        );
    }

    public static NotificationEntity toEntity(Notification domain) {
        NotificationEntity entity = new NotificationEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.type = domain.getType().name();
        entity.title = domain.getTitle();
        entity.body = domain.getBody();
        entity.relatedEntityId = domain.getRelatedEntityId();
        entity.isRead = domain.isRead();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
