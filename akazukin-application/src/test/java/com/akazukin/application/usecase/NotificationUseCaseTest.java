package com.akazukin.application.usecase;

import com.akazukin.application.dto.NotificationDto;
import com.akazukin.domain.model.Notification;
import com.akazukin.domain.model.NotificationType;
import com.akazukin.domain.port.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationUseCaseTest {

    private InMemoryNotificationRepository notificationRepository;
    private NotificationUseCase notificationUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        notificationRepository = new InMemoryNotificationRepository();
        notificationUseCase = new NotificationUseCase(notificationRepository);

        userId = UUID.randomUUID();
    }

    @Test
    void listNotifications_returnsPaginatedResults() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Title 1", false);
        saveNotification(userId, NotificationType.POST_FAILED, "Title 2", false);
        saveNotification(userId, NotificationType.SYSTEM, "Title 3", true);

        List<NotificationDto> result = notificationUseCase.listNotifications(userId, 0, 2);

        assertEquals(2, result.size());
    }

    @Test
    void listNotifications_returnsSecondPage() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Title 1", false);
        saveNotification(userId, NotificationType.POST_FAILED, "Title 2", false);
        saveNotification(userId, NotificationType.SYSTEM, "Title 3", false);

        List<NotificationDto> result = notificationUseCase.listNotifications(userId, 1, 2);

        assertEquals(1, result.size());
    }

    @Test
    void listNotifications_returnsEmptyForUserWithNoNotifications() {
        UUID otherUserId = UUID.randomUUID();

        List<NotificationDto> result = notificationUseCase.listNotifications(otherUserId, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void listNotifications_mapsDtoFieldsCorrectly() {
        UUID relatedId = UUID.randomUUID();
        Notification notification = new Notification(
                UUID.randomUUID(), userId, NotificationType.APPROVAL_REQUESTED,
                "Approval Needed", "Please review post", relatedId, false, Instant.now());
        notificationRepository.save(notification);

        List<NotificationDto> result = notificationUseCase.listNotifications(userId, 0, 10);

        assertEquals(1, result.size());
        NotificationDto dto = result.get(0);
        assertEquals("APPROVAL_REQUESTED", dto.type());
        assertEquals("Approval Needed", dto.title());
        assertEquals("Please review post", dto.body());
        assertEquals(relatedId, dto.relatedEntityId());
        assertFalse(dto.isRead());
    }

    @Test
    void listUnread_returnsOnlyUnreadNotifications() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Unread 1", false);
        saveNotification(userId, NotificationType.POST_FAILED, "Read 1", true);
        saveNotification(userId, NotificationType.SYSTEM, "Unread 2", false);

        List<NotificationDto> result = notificationUseCase.listUnread(userId);

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(NotificationDto::isRead));
    }

    @Test
    void listUnread_returnsEmptyWhenAllRead() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Read", true);

        List<NotificationDto> result = notificationUseCase.listUnread(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void countUnread_returnsCorrectCount() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Unread 1", false);
        saveNotification(userId, NotificationType.POST_FAILED, "Read 1", true);
        saveNotification(userId, NotificationType.SYSTEM, "Unread 2", false);

        long count = notificationUseCase.countUnread(userId);

        assertEquals(2, count);
    }

    @Test
    void countUnread_returnsZeroWhenAllRead() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Read", true);

        long count = notificationUseCase.countUnread(userId);

        assertEquals(0, count);
    }

    @Test
    void countUnread_returnsZeroForUserWithNoNotifications() {
        UUID otherUserId = UUID.randomUUID();

        long count = notificationUseCase.countUnread(otherUserId);

        assertEquals(0, count);
    }

    @Test
    void markAsRead_marksSpecificNotificationAsRead() {
        Notification notification = saveNotification(userId, NotificationType.POST_PUBLISHED, "Title", false);

        notificationUseCase.markAsRead(notification.getId());

        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertTrue(updated.isRead());
    }

    @Test
    void markAsRead_doesNotAffectOtherNotifications() {
        Notification n1 = saveNotification(userId, NotificationType.POST_PUBLISHED, "Title 1", false);
        Notification n2 = saveNotification(userId, NotificationType.POST_FAILED, "Title 2", false);

        notificationUseCase.markAsRead(n1.getId());

        Notification updatedN2 = notificationRepository.findById(n2.getId()).orElseThrow();
        assertFalse(updatedN2.isRead());
    }

    @Test
    void markAllAsRead_marksAllNotificationsForUserAsRead() {
        saveNotification(userId, NotificationType.POST_PUBLISHED, "Title 1", false);
        saveNotification(userId, NotificationType.POST_FAILED, "Title 2", false);
        saveNotification(userId, NotificationType.SYSTEM, "Title 3", false);

        notificationUseCase.markAllAsRead(userId);

        long unread = notificationUseCase.countUnread(userId);
        assertEquals(0, unread);
    }

    @Test
    void markAllAsRead_doesNotAffectOtherUsers() {
        UUID otherUserId = UUID.randomUUID();
        saveNotification(userId, NotificationType.POST_PUBLISHED, "User Title", false);
        saveNotification(otherUserId, NotificationType.POST_FAILED, "Other Title", false);

        notificationUseCase.markAllAsRead(userId);

        long userUnread = notificationUseCase.countUnread(userId);
        long otherUnread = notificationUseCase.countUnread(otherUserId);
        assertEquals(0, userUnread);
        assertEquals(1, otherUnread);
    }

    private Notification saveNotification(UUID targetUserId, NotificationType type,
                                          String title, boolean isRead) {
        Notification notification = new Notification(
                UUID.randomUUID(), targetUserId, type, title, "body",
                null, isRead, Instant.now());
        return notificationRepository.save(notification);
    }

    private static class InMemoryNotificationRepository implements NotificationRepository {

        private final Map<UUID, Notification> store = new HashMap<>();

        @Override
        public Optional<Notification> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Notification> findByUserId(UUID userId, int offset, int limit) {
            List<Notification> userNotifications = store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .toList();

            int end = Math.min(offset + limit, userNotifications.size());
            if (offset >= userNotifications.size()) {
                return List.of();
            }
            return new ArrayList<>(userNotifications.subList(offset, end));
        }

        @Override
        public List<Notification> findUnreadByUserId(UUID userId) {
            return store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> !n.isRead())
                    .toList();
        }

        @Override
        public Notification save(Notification notification) {
            store.put(notification.getId(), notification);
            return notification;
        }

        @Override
        public void markAsRead(UUID id) {
            Notification notification = store.get(id);
            if (notification != null) {
                notification.setRead(true);
            }
        }

        @Override
        public void markAllAsReadByUserId(UUID userId) {
            store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .forEach(n -> n.setRead(true));
        }

        @Override
        public long countUnreadByUserId(UUID userId) {
            return store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> !n.isRead())
                    .count();
        }
    }
}
