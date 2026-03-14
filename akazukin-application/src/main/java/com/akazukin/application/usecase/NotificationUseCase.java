package com.akazukin.application.usecase;

import com.akazukin.application.dto.NotificationDto;
import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class NotificationUseCase {

    private static final Logger LOG = Logger.getLogger(NotificationUseCase.class.getName());

    private final NotificationRepository notificationRepository;

    @Inject
    public NotificationUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDto> listNotifications(UUID userId, int page, int size) {
        int offset = page * size;
        return notificationRepository.findByUserId(userId, offset, size).stream()
                .map(this::toNotificationDto)
                .toList();
    }

    public List<NotificationDto> listUnread(UUID userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(this::toNotificationDto)
                .toList();
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId);
        LOG.log(Level.FINE, "Notification {0} marked as read", notificationId);
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        LOG.log(Level.INFO, "All notifications marked as read for user {0}", userId);
    }

    private NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRelatedEntityId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
