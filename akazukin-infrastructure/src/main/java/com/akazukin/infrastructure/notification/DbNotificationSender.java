package com.akazukin.infrastructure.notification;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationRepository;
import com.akazukin.domain.port.NotificationSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DbNotificationSender implements NotificationSender {
    private static final Logger LOG = Logger.getLogger(DbNotificationSender.class.getName());

    private final NotificationRepository notificationRepository;

    @Inject
    public DbNotificationSender(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void send(Notification notification) {
        notificationRepository.save(notification);
        LOG.log(Level.FINE, "Notification saved for user {0}: {1}",
            new Object[]{notification.getUserId(), notification.getTitle()});
    }
}
