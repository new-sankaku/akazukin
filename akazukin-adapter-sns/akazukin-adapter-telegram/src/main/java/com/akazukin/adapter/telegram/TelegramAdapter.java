package com.akazukin.adapter.telegram;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

public class TelegramAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String BOT_FATHER_URL = "https://t.me/BotFather";
    private static final String API_BASE = "https://api.telegram.org";

    private final String defaultChatId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TelegramAdapter(String defaultChatId, HttpClient httpClient, ObjectMapper objectMapper) {
        this.defaultChatId = Objects.requireNonNull(defaultChatId, "defaultChatId must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public TelegramAdapter(String defaultChatId) {
        this(defaultChatId, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public TelegramAdapter() {
        this(
            System.getProperty("akazukin.telegram.chat-id",
                System.getenv().getOrDefault("TELEGRAM_CHAT_ID", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.TELEGRAM;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return BOT_FATHER_URL;
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            recordApiCall();
            return new SnsAuthToken(code, null, null, null);
        } catch (RuntimeException e) {
            throw wrapException("exchangeToken", e);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        try {
            checkRateLimit();
            recordApiCall();
            return new SnsAuthToken(refreshToken, null, null, null);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        try {
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/bot" + accessToken + "/getMe"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            checkTelegramResult(json, "getProfile");
            recordApiCall();

            JsonNode result = json.path("result");
            String username = result.path("username").asText("");

            return new SnsProfile(
                username,
                result.path("first_name").asText("")
                    + (result.has("last_name") ? " " + result.path("last_name").asText("") : ""),
                null,
                0
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getProfile", e);
        } catch (RuntimeException e) {
            throw wrapException("getProfile", e);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        try {
            checkRateLimit();
            String content = request.content();
            String chatId;
            String text;

            int sep = content.indexOf(':');
            if (sep > 0) {
                chatId = content.substring(0, sep).trim();
                text = content.substring(sep + 1).trim();
            } else {
                chatId = defaultChatId;
                text = content;
            }

            if (chatId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Telegram chat_id is required. Either set a default chat ID or format content as "
                        + "'chat_id:message_text'."
                );
            }

            String body = "chat_id=" + encode(chatId) + "&text=" + encode(text);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/bot" + accessToken + "/sendMessage"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            checkTelegramResult(json, "post");
            recordApiCall();

            JsonNode result = json.path("result");
            String messageId = String.valueOf(result.path("message_id").asInt());
            String postUrl = "https://t.me/c/" + chatId + "/" + messageId;

            return new PostResult(
                chatId + ":" + messageId,
                postUrl,
                Instant.now()
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("post", e);
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        try {
            checkRateLimit();
            String chatId;
            String messageId;

            int sep = postId.indexOf(':');
            if (sep > 0) {
                chatId = postId.substring(0, sep);
                messageId = postId.substring(sep + 1);
            } else {
                chatId = defaultChatId;
                messageId = postId;
            }

            String body = "chat_id=" + encode(chatId) + "&message_id=" + encode(messageId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/bot" + accessToken + "/deleteMessage"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "deletePost");
            JsonNode json = objectMapper.readTree(response.body());
            checkTelegramResult(json, "deletePost");
            recordApiCall();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("deletePost", e);
        } catch (RuntimeException e) {
            throw wrapException("deletePost", e);
        }
    }

    private void checkTelegramResult(JsonNode json, String operation) {
        if (!json.path("ok").asBoolean(false)) {
            String description = json.path("description").asText("Unknown error");
            int errorCode = json.path("error_code").asInt(0);
            throw wrapException(operation,
                new RuntimeException("Telegram API error " + errorCode + ": " + description));
        }
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
