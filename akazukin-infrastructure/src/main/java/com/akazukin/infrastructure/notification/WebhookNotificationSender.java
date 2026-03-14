package com.akazukin.infrastructure.notification;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationSender;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class WebhookNotificationSender implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(WebhookNotificationSender.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @ConfigProperty(name = "akazukin.notification.webhook.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "akazukin.notification.webhook.url", defaultValue = "")
    String webhookUrl;

    @Override
    public void send(Notification notification) {
        if (!enabled || webhookUrl.isBlank()) {
            return;
        }

        String json = buildPayload(notification);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        CompletableFuture<HttpResponse<String>> future = HTTP_CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        future.whenComplete((response, throwable) -> {
            if (throwable != null) {
                LOG.errorf(throwable, "Failed to send webhook notification: %s", notification.getTitle());
            } else if (response.statusCode() >= 400) {
                LOG.errorf("Webhook returned error status %d for notification: %s",
                        response.statusCode(), notification.getTitle());
            } else {
                LOG.infof("Webhook notification sent: %s", notification.getTitle());
            }
        });
    }

    private String buildPayload(Notification notification) {
        return "{" +
                "\"type\":\"" + escapeJson(notification.getType().name()) + "\"," +
                "\"title\":\"" + escapeJson(notification.getTitle()) + "\"," +
                "\"message\":\"" + escapeJson(notification.getMessage()) + "\"," +
                "\"userId\":\"" + notification.getUserId() + "\"" +
                "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
