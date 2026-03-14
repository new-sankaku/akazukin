package com.akazukin.adapter.mastodon;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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

@DisplayName("MastodonAdapter WireMock Tests")
class MastodonAdapterWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private MastodonAdapter adapter;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        adapter = new MastodonAdapter(baseUrl, "test-client-id", "test-client-secret",
            HTTP_CLIENT, OBJECT_MAPPER);
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns MASTODON")
        void platform_returnsMastodon() {
            assertEquals(SnsPlatform.MASTODON, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 500")
        void getMaxContentLength_returns500() {
            assertEquals(500, adapter.getMaxContentLength());
        }
    }

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("contains client_id")
        void containsClientId() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("client_id=test-client-id"));
        }

        @Test
        @DisplayName("contains state")
        void containsState() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "mystate");
            assertTrue(url.contains("state=mystate"));
        }

        @Test
        @DisplayName("contains response_type=code")
        void containsResponseType() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("response_type=code"));
        }

        @Test
        @DisplayName("contains redirect_uri")
        void containsRedirectUri() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("redirect_uri="));
        }

        @Test
        @DisplayName("contains scope read write")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("starts with instance URL")
        void startsWithInstanceUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith(wireMock.baseUrl() + "/oauth/authorize"));
        }
    }

    @Nested
    @DisplayName("exchangeToken()")
    class ExchangeTokenTests {

        @Test
        @DisplayName("success returns SnsAuthToken")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "access_token": "mastodon-access-token",
                            "token_type": "Bearer",
                            "scope": "read write",
                            "created_at": 1710000000
                        }
                        """)));

            SnsAuthToken token = adapter.exchangeToken("auth-code-123", "https://callback.example.com");

            assertNotNull(token);
            assertEquals("mastodon-access-token", token.accessToken());
            assertEquals("read write", token.scope());
        }

        @Test
        @DisplayName("sends grant_type=authorization_code in body")
        void sendsGrantType() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","scope":"s"}
                        """)));

            adapter.exchangeToken("code", "https://callback.example.com");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("grant_type=authorization_code")));
        }

        @Test
        @DisplayName("sends code in body")
        void sendsCode() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","scope":"s"}
                        """)));

            adapter.exchangeToken("mycode", "https://callback.example.com");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("code=mycode")));
        }

        @Test
        @DisplayName("sends client_id and client_secret in body")
        void sendsClientCredentials() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","scope":"s"}
                        """)));

            adapter.exchangeToken("code", "https://callback.example.com");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("client_id=test-client-id"))
                .withRequestBody(containing("client_secret=test-client-secret")));
        }

        @Test
        @DisplayName("sends Content-Type application/x-www-form-urlencoded")
        void sendsFormContentType() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","scope":"s"}
                        """)));

            adapter.exchangeToken("code", "https://callback.example.com");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
        }

        @Test
        @DisplayName("with expires_in sets expiresAt")
        void withExpiresIn_setsExpiresAt() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","expires_in":3600,"scope":"s"}
                        """)));

            SnsAuthToken token = adapter.exchangeToken("code", "https://callback.example.com");
            assertNotNull(token.expiresAt());
            assertTrue(token.expiresAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("without expires_in sets expiresAt to null")
        void withoutExpiresIn_expiresAtIsNull() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"t","scope":"s"}
                        """)));

            SnsAuthToken token = adapter.exchangeToken("code", "https://callback.example.com");
            assertNull(token.expiresAt());
        }

        @Test
        @DisplayName("with refresh_token returns it")
        void withRefreshToken() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","refresh_token":"r","scope":"s"}
                        """)));

            SnsAuthToken token = adapter.exchangeToken("code", "https://callback.example.com");
            assertEquals("r", token.refreshToken());
        }

        @Test
        @DisplayName("400 error throws SnsApiException")
        void badRequest_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("invalid_grant")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("bad-code", "https://callback.example.com"));
            assertEquals(SnsPlatform.MASTODON, ex.getPlatform());
        }

        @Test
        @DisplayName("401 error throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("unauthorized")));

            assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("code", "https://callback.example.com"));
        }

        @Test
        @DisplayName("500 error throws SnsApiException")
        void serverError_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("code", "https://callback.example.com"));
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("success returns new SnsAuthToken")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "access_token": "new-access",
                            "refresh_token": "new-refresh",
                            "scope": "read write",
                            "expires_in": 7200
                        }
                        """)));

            SnsAuthToken token = adapter.refreshToken("old-refresh-token");

            assertEquals("new-access", token.accessToken());
            assertEquals("new-refresh", token.refreshToken());
            assertEquals("read write", token.scope());
            assertNotNull(token.expiresAt());
        }

        @Test
        @DisplayName("sends grant_type=refresh_token")
        void sendsRefreshGrantType() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","scope":"s"}
                        """)));

            adapter.refreshToken("rt");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(containing("refresh_token=rt")));
        }

        @Test
        @DisplayName("sends client_id and client_secret")
        void sendsClientCredentials() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","scope":"s"}
                        """)));

            adapter.refreshToken("rt");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("client_id=test-client-id"))
                .withRequestBody(containing("client_secret=test-client-secret")));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("invalid_token")));

            assertThrows(SnsApiException.class, () -> adapter.refreshToken("bad"));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Server Error")));

            assertThrows(SnsApiException.class, () -> adapter.refreshToken("rt"));
        }
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("success returns SnsProfile with all fields")
        void success() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "id": "12345",
                            "acct": "testuser",
                            "display_name": "Test User",
                            "avatar": "https://mastodon.social/avatars/test.png",
                            "followers_count": 100
                        }
                        """)));

            SnsProfile profile = adapter.getProfile("access-token");

            assertEquals("testuser", profile.accountIdentifier());
            assertEquals("Test User", profile.displayName());
            assertEquals("https://mastodon.social/avatars/test.png", profile.avatarUrl());
            assertEquals(100, profile.followerCount());
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"acct":"u","display_name":"d","followers_count":0}
                        """)));

            adapter.getProfile("my-token");

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .withHeader("Authorization", equalTo("Bearer my-token")));
        }

        @Test
        @DisplayName("missing avatar returns null")
        void missingAvatar() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"acct":"u","display_name":"d","followers_count":0}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertNull(profile.avatarUrl());
        }

        @Test
        @DisplayName("missing display_name defaults to empty")
        void missingDisplayName() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"acct":"u","followers_count":0}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertEquals("", profile.displayName());
        }

        @Test
        @DisplayName("zero followers")
        void zeroFollowers() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"acct":"u","display_name":"d","followers_count":0}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertEquals(0, profile.followerCount());
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class, () -> adapter.getProfile("bad-token"));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class, () -> adapter.getProfile("token"));
        }

        @Test
        @DisplayName("profile with Japanese display_name")
        void japaneseDisplayName() {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/accounts/verify_credentials"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("""
                        {"acct":"u","display_name":"\u30c6\u30b9\u30c8","followers_count":5}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertEquals("\u30c6\u30b9\u30c8", profile.displayName());
        }
    }

    @Nested
    @DisplayName("post()")
    class PostTests {

        @Test
        @DisplayName("success returns PostResult")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "id": "109876543210",
                            "url": "https://mastodon.social/@testuser/109876543210",
                            "created_at": "2026-03-13T12:00:00.000Z"
                        }
                        """)));

            PostResult result = adapter.post("token", new PostRequest("Hello Mastodon!", List.of()));

            assertEquals("109876543210", result.platformPostId());
            assertEquals("https://mastodon.social/@testuser/109876543210", result.platformUrl());
            assertNotNull(result.publishedAt());
        }

        @Test
        @DisplayName("sends form-encoded body with status param")
        void sendsFormEncodedBody() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"https://m.s/@u/1","created_at":"2026-03-13T12:00:00.000Z"}
                        """)));

            adapter.post("token", new PostRequest("Hello!", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v1/statuses"))
                .withRequestBody(containing("status=Hello")));
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"https://m.s/@u/1","created_at":"2026-03-13T12:00:00.000Z"}
                        """)));

            adapter.post("my-token", new PostRequest("test", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v1/statuses"))
                .withHeader("Authorization", equalTo("Bearer my-token")));
        }

        @Test
        @DisplayName("parses created_at as publishedAt")
        void parsesCreatedAt() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"u","created_at":"2026-01-15T10:30:00.000Z"}
                        """)));

            PostResult result = adapter.post("token", new PostRequest("test", List.of()));
            assertEquals(Instant.parse("2026-01-15T10:30:00.000Z"), result.publishedAt());
        }

        @Test
        @DisplayName("missing created_at uses current time")
        void missingCreatedAt() {
            Instant before = Instant.now();
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"u"}
                        """)));

            PostResult result = adapter.post("token", new PostRequest("test", List.of()));
            assertTrue(result.publishedAt().isAfter(before.minusSeconds(1)));
        }

        @Test
        @DisplayName("missing url uses fallback format")
        void missingUrl_fallback() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"123","created_at":"2026-03-13T12:00:00.000Z"}
                        """)));

            PostResult result = adapter.post("token", new PostRequest("test", List.of()));
            assertTrue(result.platformUrl().contains("/@unknown/123"));
        }

        @Test
        @DisplayName("Japanese text post")
        void japaneseText() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"u","created_at":"2026-03-13T12:00:00.000Z"}
                        """)));

            PostResult result = adapter.post("token",
                new PostRequest("\u3053\u3093\u306b\u3061\u306f", List.of()));
            assertNotNull(result);
        }

        @Test
        @DisplayName("max length (500 chars) text post")
        void maxLengthText() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"id":"1","url":"u","created_at":"2026-03-13T12:00:00.000Z"}
                        """)));

            PostResult result = adapter.post("token",
                new PostRequest("a".repeat(500), List.of()));
            assertNotNull(result);
        }

        @Test
        @DisplayName("400 throws SnsApiException")
        void badRequest() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse().withStatus(400).withBody("Bad request")));

            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("422 throws SnsApiException")
        void unprocessableEntity() {
            wireMock.stubFor(post(urlPathEqualTo("/api/v1/statuses"))
                .willReturn(aResponse().withStatus(422).withBody("Unprocessable")));

            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("test", List.of())));
        }
    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePostTests {

        @Test
        @DisplayName("success does not throw")
        void success() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/109876543210"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

            assertDoesNotThrow(() -> adapter.deletePost("token", "109876543210"));
        }

        @Test
        @DisplayName("sends DELETE to correct URL with post ID")
        void sendsDeleteToCorrectUrl() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/my-post-id"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

            adapter.deletePost("token", "my-post-id");

            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/api/v1/statuses/my-post-id")));
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/id"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

            adapter.deletePost("my-token", "id");

            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/api/v1/statuses/id"))
                .withHeader("Authorization", equalTo("Bearer my-token")));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/id"))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

            assertThrows(SnsApiException.class, () -> adapter.deletePost("token", "id"));
        }

        @Test
        @DisplayName("404 throws SnsApiException")
        void notFound() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/nonexistent"))
                .willReturn(aResponse().withStatus(404).withBody("Not found")));

            assertThrows(SnsApiException.class, () -> adapter.deletePost("token", "nonexistent"));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError() {
            wireMock.stubFor(delete(urlPathEqualTo("/api/v1/statuses/id"))
                .willReturn(aResponse().withStatus(500).withBody("Error")));

            assertThrows(SnsApiException.class, () -> adapter.deletePost("token", "id"));
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
        @DisplayName("instanceUrl must not be null")
        void instanceUrlNotNull() {
            assertThrows(NullPointerException.class,
                () -> new MastodonAdapter(null, "cid", "csec", HTTP_CLIENT, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("clientId must not be null")
        void clientIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new MastodonAdapter("url", null, "csec", HTTP_CLIENT, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("clientSecret must not be null")
        void clientSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new MastodonAdapter("url", "cid", null, HTTP_CLIENT, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("httpClient must not be null")
        void httpClientNotNull() {
            assertThrows(NullPointerException.class,
                () -> new MastodonAdapter("url", "cid", "csec", null, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("objectMapper must not be null")
        void objectMapperNotNull() {
            assertThrows(NullPointerException.class,
                () -> new MastodonAdapter("url", "cid", "csec", HTTP_CLIENT, null));
        }
    }
}
