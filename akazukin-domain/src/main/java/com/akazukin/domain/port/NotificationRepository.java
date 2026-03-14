package com.akazukin.domain.port;

import com.akazukin.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserId(UUID userId, int offset, int limit);

    List<Notification> findUnreadByUserId(UUID userId);

    Notification save(Notification notification);

    void markAsRead(UUID id);

    void markAllAsReadByUserId(UUID userId);

    long countUnreadByUserId(UUID userId);
}
