package com.akazukin.infrastructure.notification;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationSender;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@Alternative
@Priority(1)
public class CompositeNotificationSender implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(CompositeNotificationSender.class);

    @Inject
    DbNotificationSender dbSender;

    @Inject
    EmailNotificationSender emailSender;

    @Inject
    WebhookNotificationSender webhookSender;

    @Override
    public void send(Notification notification) {
        dbSender.send(notification);
        trySend(emailSender, notification);
        trySend(webhookSender, notification);
    }

    private void trySend(NotificationSender sender, Notification notification) {
        try {
            sender.send(notification);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Notification sender %s failed for notification: %s",
                    sender.getClass().getSimpleName(), notification.getTitle());
        }
    }
}
