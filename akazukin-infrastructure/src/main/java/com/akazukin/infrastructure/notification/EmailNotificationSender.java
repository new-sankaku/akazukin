package com.akazukin.infrastructure.notification;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationSender;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailNotificationSender implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(EmailNotificationSender.class);

    @Inject
    ReactiveMailer mailer;

    @ConfigProperty(name = "akazukin.notification.email.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "akazukin.notification.email.recipient")
    String recipient;

    @Override
    public void send(Notification notification) {
        if (!enabled) {
            return;
        }

        String sanitizedTitle = notification.getTitle()
                .replace("\r", "")
                .replace("\n", "");

        Mail mail = Mail.withText(
                recipient,
                "[Akazukin] " + sanitizedTitle,
                notification.getBody()
        );

        mailer.send(mail)
                .subscribe().with(
                        success -> LOG.infof("Email notification sent: %s", notification.getTitle()),
                        failure -> LOG.errorf(failure, "Failed to send email notification: %s", notification.getTitle())
                );
    }
}
