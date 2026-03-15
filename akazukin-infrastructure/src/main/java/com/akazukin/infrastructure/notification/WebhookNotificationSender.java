package com.akazukin.infrastructure.notification;

import com.akazukin.domain.model.Notification;
import com.akazukin.domain.port.NotificationSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class WebhookNotificationSender implements NotificationSender {

    private static final Logger LOG = Logger.getLogger(WebhookNotificationSender.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ConfigProperty(name = "akazukin.notification.webhook.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "akazukin.notification.webhook.url")
    Optional<String> webhookUrl;

    @PostConstruct
    void validateConfig() {
        if (enabled && webhookUrl.isPresent() && !webhookUrl.get().isBlank()) {
            validateWebhookUrl(webhookUrl.get());
        }
    }

    private void validateWebhookUrl(String url) {
        URI uri = URI.create(url);

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Webhook URL must use http or https scheme: " + url);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must have a valid host: " + url);
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Webhook URL must not point to a private/local address: " + url);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Webhook URL host cannot be resolved: " + url, e);
        }
    }

    @Override
    public void send(Notification notification) {
        String url = webhookUrl.orElse("");
        if (!enabled || url.isBlank()) {
            return;
        }

        String json = buildPayload(notification);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
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
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", notification.getType().name());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getBody());
        payload.put("userId", String.valueOf(notification.getUserId()));

        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize webhook payload", e);
        }
    }
}
