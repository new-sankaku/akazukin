package com.akazukin.adapter.telegram;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TelegramAdapter WireMock Tests")
class TelegramAdapterWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private static final String BOT_TOKEN = "123456:ABC-DEF";
    private static final String DEFAULT_CHAT_ID = "987654321";

    private TelegramAdapter adapter;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        adapter = new TelegramAdapter(DEFAULT_CHAT_ID, HTTP_CLIENT, OBJECT_MAPPER, baseUrl);
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns TELEGRAM")
        void platform_returnsTelegram() {
            assertEquals(SnsPlatform.TELEGRAM, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 4096")
        void returnsDefault() {
            assertEquals(4096, adapter.getMaxContentLength());
        }
    }

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("returns BotFather URL")
        void returnsBotFatherUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertEquals("https://t.me/BotFather", url);
        }

        @Test
        @DisplayName("ignores callbackUrl")
        void ignoresCallbackUrl() {
            String url = adapter.getAuthorizationUrl("https://other.example.com", "s");
            assertEquals("https://t.me/BotFather", url);
        }

        @Test
        @DisplayName("ignores state")
        void ignoresState() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "any-state");
            assertEquals("https://t.me/BotFather", url);
        }
    }

    @Nested
    @DisplayName("exchangeToken()")
    class ExchangeTokenTests {

        @Test
        @DisplayName("returns bot token as access token passthrough")
        void botTokenPassthrough() {
            SnsAuthToken token = adapter.exchangeToken("bot-token-123", "https://callback.example.com");

            assertNotNull(token);
            assertEquals("bot-token-123", token.accessToken());
            assertNull(token.refreshToken());
            assertNull(token.expiresAt());
            assertNull(token.scope());
        }

        @Test
        @DisplayName("different code values returned as-is")
        void differentCodeValues() {
            SnsAuthToken token = adapter.exchangeToken("999:XYZ-ABC", "callback");
            assertEquals("999:XYZ-ABC", token.accessToken());
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("returns same token as access token passthrough")
        void tokenPassthrough() {
            SnsAuthToken token = adapter.refreshToken("my-bot-token");

            assertNotNull(token);
            assertEquals("my-bot-token", token.accessToken());
            assertNull(token.refreshToken());
            assertNull(token.expiresAt());
            assertNull(token.scope());
        }
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("success returns SnsProfile from getMe")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "ok": true,
                            "result": {
                                "id": 123456789,
                                "is_bot": true,
                                "first_name": "Test Bot",
                                "last_name": "v2",
                                "username": "test_bot"
                            }
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(BOT_TOKEN);

            assertEquals("test_bot", profile.accountIdentifier());
            assertEquals("Test Bot v2", profile.displayName());
            assertNull(profile.avatarUrl());
            assertEquals(0, profile.followerCount());
        }

        @Test
        @DisplayName("bot without last_name")
        void withoutLastName() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "ok": true,
                            "result": {
                                "id": 123456789,
                                "is_bot": true,
                                "first_name": "MyBot",
                                "username": "my_bot"
                            }
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(BOT_TOKEN);
            assertEquals("MyBot", profile.displayName());
        }

        @Test
        @DisplayName("followerCount is always 0 for bots")
        void followerCountAlwaysZero() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"id":1,"first_name":"B","username":"b"}}
                        """)));

            SnsProfile profile = adapter.getProfile(BOT_TOKEN);
            assertEquals(0, profile.followerCount());
        }

        @Test
        @DisplayName("avatarUrl is always null")
        void avatarAlwaysNull() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"id":1,"first_name":"B","username":"b"}}
                        """)));

            SnsProfile profile = adapter.getProfile(BOT_TOKEN);
            assertNull(profile.avatarUrl());
        }

        @Test
        @DisplayName("ok=false throws SnsApiException")
        void okFalse_throwsException() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":false,"error_code":401,"description":"Unauthorized"}
                        """)));

            assertThrows(SnsApiException.class, () -> adapter.getProfile(BOT_TOKEN));
        }

        @Test
        @DisplayName("401 HTTP status throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class, () -> adapter.getProfile(BOT_TOKEN));
        }

        @Test
        @DisplayName("500 HTTP status throws SnsApiException")
        void serverError() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class, () -> adapter.getProfile(BOT_TOKEN));
        }

        @Test
        @DisplayName("uses POST method for getMe")
        void usesPostMethod() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"id":1,"first_name":"B","username":"b"}}
                        """)));

            adapter.getProfile(BOT_TOKEN);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/bot" + BOT_TOKEN + "/getMe")));
        }
    }

    @Nested
    @DisplayName("post()")
    class PostTests {

        @Test
        @DisplayName("success with chatId:text format")
        void successWithChatIdFormat() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "ok": true,
                            "result": {
                                "message_id": 42,
                                "chat": {"id": 987654321},
                                "text": "Hello!"
                            }
                        }
                        """)));

            PostResult result = adapter.post(BOT_TOKEN,
                new PostRequest(DEFAULT_CHAT_ID + ":Hello Telegram!", List.of()));

            assertNotNull(result);
            assertEquals(DEFAULT_CHAT_ID + ":42", result.platformPostId());
            assertNotNull(result.platformUrl());
            assertNotNull(result.publishedAt());
        }

        @Test
        @DisplayName("text without chatId prefix uses default chatId")
        void usesDefaultChatId() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":10,"chat":{"id":987654321},"text":"hi"}}
                        """)));

            PostResult result = adapter.post(BOT_TOKEN,
                new PostRequest("no-colon-here", List.of()));

            assertNotNull(result);
        }

        @Test
        @DisplayName("sends chat_id and text in form body")
        void sendsChatIdAndText() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":1,"chat":{"id":1},"text":"t"}}
                        """)));

            adapter.post(BOT_TOKEN, new PostRequest("12345:My message", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .withRequestBody(containing("chat_id=12345"))
                .withRequestBody(containing("text=My+message")));
        }

        @Test
        @DisplayName("platformPostId format is chatId:messageId")
        void platformPostIdFormat() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":99,"chat":{"id":555},"text":"t"}}
                        """)));

            PostResult result = adapter.post(BOT_TOKEN,
                new PostRequest("555:text", List.of()));
            assertEquals("555:99", result.platformPostId());
        }

        @Test
        @DisplayName("platformUrl format")
        void platformUrlFormat() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":42,"chat":{"id":1},"text":"t"}}
                        """)));

            PostResult result = adapter.post(BOT_TOKEN,
                new PostRequest("mychat:text", List.of()));
            assertTrue(result.platformUrl().startsWith("https://t.me/c/"));
        }

        @Test
        @DisplayName("empty chatId throws IllegalArgumentException")
        void emptyChatId_throwsException() {
            TelegramAdapter emptyAdapter = new TelegramAdapter("", HTTP_CLIENT, OBJECT_MAPPER,
                wireMock.baseUrl());

            assertThrows(SnsApiException.class,
                () -> emptyAdapter.post(BOT_TOKEN, new PostRequest("no-colon", List.of())));
        }

        @Test
        @DisplayName("ok=false throws SnsApiException")
        void okFalse_throwsException() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}
                        """)));

            assertThrows(SnsApiException.class,
                () -> adapter.post(BOT_TOKEN, new PostRequest(DEFAULT_CHAT_ID + ":text", List.of())));
        }

        @Test
        @DisplayName("400 throws SnsApiException")
        void badRequest() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse().withStatus(400).withBody("Bad request")));

            assertThrows(SnsApiException.class,
                () -> adapter.post(BOT_TOKEN, new PostRequest(DEFAULT_CHAT_ID + ":text", List.of())));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class,
                () -> adapter.post(BOT_TOKEN, new PostRequest(DEFAULT_CHAT_ID + ":text", List.of())));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.post(BOT_TOKEN, new PostRequest(DEFAULT_CHAT_ID + ":text", List.of())));
        }

        @Test
        @DisplayName("Japanese text post")
        void japaneseText() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":1,"chat":{"id":1},"text":"t"}}
                        """)));

            PostResult result = adapter.post(BOT_TOKEN,
                new PostRequest(DEFAULT_CHAT_ID + ":\u3053\u3093\u306b\u3061\u306f", List.of()));
            assertNotNull(result);
        }

        @Test
        @DisplayName("colon in text is not treated as separator (only first colon)")
        void colonInText() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":{"message_id":1,"chat":{"id":1},"text":"t"}}
                        """)));

            adapter.post(BOT_TOKEN, new PostRequest("12345:key:value text", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/bot" + BOT_TOKEN + "/sendMessage"))
                .withRequestBody(containing("chat_id=12345"))
                .withRequestBody(containing("text=key")));
        }
    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePostTests {

        @Test
        @DisplayName("success with chatId:messageId format")
        void successWithFormat() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":true}
                        """)));

            assertDoesNotThrow(() -> adapter.deletePost(BOT_TOKEN, DEFAULT_CHAT_ID + ":42"));
        }

        @Test
        @DisplayName("sends chat_id and message_id in form body")
        void sendsChatIdAndMessageId() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":true}
                        """)));

            adapter.deletePost(BOT_TOKEN, "12345:99");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .withRequestBody(containing("chat_id=12345"))
                .withRequestBody(containing("message_id=99")));
        }

        @Test
        @DisplayName("postId without colon uses default chatId")
        void withoutColon_usesDefaultChatId() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":true,"result":true}
                        """)));

            adapter.deletePost(BOT_TOKEN, "42");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .withRequestBody(containing("chat_id=" + DEFAULT_CHAT_ID))
                .withRequestBody(containing("message_id=42")));
        }

        @Test
        @DisplayName("ok=false throws SnsApiException")
        void okFalse_throwsException() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"ok":false,"error_code":400,"description":"message to delete not found"}
                        """)));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(BOT_TOKEN, DEFAULT_CHAT_ID + ":999"));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(BOT_TOKEN, DEFAULT_CHAT_ID + ":42"));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError() {
            wireMock.stubFor(post(urlPathEqualTo("/bot" + BOT_TOKEN + "/deleteMessage"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(BOT_TOKEN, DEFAULT_CHAT_ID + ":42"));
        }
    }

    @Nested
    @DisplayName("Constructor and close()")
    class ConstructorTests {

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }

        @Test
        @DisplayName("defaultChatId must not be null")
        void defaultChatIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new TelegramAdapter(null, HTTP_CLIENT, OBJECT_MAPPER, "base"));
        }

        @Test
        @DisplayName("httpClient must not be null")
        void httpClientNotNull() {
            assertThrows(NullPointerException.class,
                () -> new TelegramAdapter("chatId", null, OBJECT_MAPPER, "base"));
        }

        @Test
        @DisplayName("objectMapper must not be null")
        void objectMapperNotNull() {
            assertThrows(NullPointerException.class,
                () -> new TelegramAdapter("chatId", HTTP_CLIENT, null, "base"));
        }

        @Test
        @DisplayName("apiBase must not be null")
        void apiBaseNotNull() {
            assertThrows(NullPointerException.class,
                () -> new TelegramAdapter("chatId", HTTP_CLIENT, OBJECT_MAPPER, null));
        }
    }
}
