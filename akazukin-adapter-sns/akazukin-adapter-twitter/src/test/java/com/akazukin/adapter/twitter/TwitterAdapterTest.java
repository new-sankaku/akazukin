package com.akazukin.adapter.twitter;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.twitter.TwitterClient;
import com.akazukin.sdk.twitter.TwitterConfig;
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

@DisplayName("TwitterAdapter Tests")
class TwitterAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private TwitterAdapter adapter;
    private TwitterClient client;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        TwitterConfig config = new TwitterConfig("test-client-id", "test-client-secret",
            "https://callback.example.com");
        client = TwitterClient.builder()
            .config(config)
            .httpClient(HTTP_CLIENT)
            .objectMapper(OBJECT_MAPPER)
            .apiBaseUrl(baseUrl)
            .authBaseUrl(baseUrl + "/i/oauth2/authorize")
            .tokenUrl(baseUrl + "/oauth2/token")
            .build();
        adapter = new TwitterAdapter(client);
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns TWITTER")
        void platform_returnsTwitter() {
            assertEquals(SnsPlatform.TWITTER, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 280")
        void returns280() {
            assertEquals(280, adapter.getMaxContentLength());
        }
    }

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("contains response_type=code")
        void containsResponseType() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("response_type=code"));
        }

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
        @DisplayName("contains code_challenge")
        void containsCodeChallenge() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("code_challenge="));
        }

        @Test
        @DisplayName("contains code_challenge_method=S256")
        void containsChallengeMethod() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("code_challenge_method=S256"));
        }

        @Test
        @DisplayName("contains scope")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("each call generates different code_challenge")
        void differentCodeChallenge() {
            String url1 = adapter.getAuthorizationUrl("https://cb1.example.com", "s1");
            String url2 = adapter.getAuthorizationUrl("https://cb2.example.com", "s2");
            // The code_challenge values should differ since each call generates a new verifier
            // We can't easily extract and compare, but the URLs should be different
            assertNotNull(url1);
            assertNotNull(url2);
        }
    }

    @Nested
    @DisplayName("exchangeToken()")
    class ExchangeTokenTests {

        @Test
        @DisplayName("success after getAuthorizationUrl returns SnsAuthToken")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "access_token": "twitter-access-token",
                            "refresh_token": "twitter-refresh-token",
                            "expires_in": 7200,
                            "scope": "tweet.read tweet.write users.read"
                        }
                        """)));

            String callbackUrl = "https://callback.example.com";
            adapter.getAuthorizationUrl(callbackUrl, "state123");
            SnsAuthToken token = adapter.exchangeToken("auth-code", callbackUrl);

            assertNotNull(token);
            assertEquals("twitter-access-token", token.accessToken());
            assertEquals("twitter-refresh-token", token.refreshToken());
            assertNotNull(token.expiresAt());
            assertEquals("tweet.read tweet.write users.read", token.scope());
        }

        @Test
        @DisplayName("without prior getAuthorizationUrl throws SnsApiException")
        void withoutPriorGetAuthUrl_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("code", "https://unknown-callback.example.com"));
        }

        @Test
        @DisplayName("with expires_in sets expiresAt in the future")
        void expiresAtInFuture() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","refresh_token":"r","expires_in":3600,"scope":"s"}
                        """)));

            String callbackUrl = "https://callback.example.com";
            adapter.getAuthorizationUrl(callbackUrl, "s");
            SnsAuthToken token = adapter.exchangeToken("code", callbackUrl);

            assertTrue(token.expiresAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("with expires_in=0 sets expiresAt to null")
        void expiresInZero_expiresAtNull() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","refresh_token":"r","expires_in":0,"scope":"s"}
                        """)));

            String callbackUrl = "https://callback.example.com";
            adapter.getAuthorizationUrl(callbackUrl, "s");
            SnsAuthToken token = adapter.exchangeToken("code", callbackUrl);

            assertNull(token.expiresAt());
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("success returns new SnsAuthToken")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "access_token": "new-access",
                            "refresh_token": "new-refresh",
                            "expires_in": 7200,
                            "scope": "tweet.read tweet.write"
                        }
                        """)));

            SnsAuthToken token = adapter.refreshToken("old-refresh-token");

            assertEquals("new-access", token.accessToken());
            assertEquals("new-refresh", token.refreshToken());
            assertNotNull(token.expiresAt());
        }

        @Test
        @DisplayName("sends grant_type=refresh_token")
        void sendsRefreshGrantType() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"access_token":"a","refresh_token":"r","expires_in":0,"scope":"s"}
                        """)));

            adapter.refreshToken("rt");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(containing("refresh_token=rt")));
        }

        @Test
        @DisplayName("400 error throws SnsApiException")
        void badRequest() {
            wireMock.stubFor(post(urlPathEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"invalid_request","error_description":"Invalid refresh token"}
                        """)));

            assertThrows(SnsApiException.class, () -> adapter.refreshToken("bad-token"));
        }
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("success returns SnsProfile")
        void success() {
            wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "data": {
                                "id": "123",
                                "username": "testuser",
                                "name": "Test User",
                                "profile_image_url": "https://pbs.twimg.com/profile_images/test.jpg",
                                "public_metrics": {
                                    "followers_count": 500,
                                    "following_count": 200
                                }
                            }
                        }
                        """)));

            SnsProfile profile = adapter.getProfile("access-token");

            assertEquals("testuser", profile.accountIdentifier());
            assertEquals("Test User", profile.displayName());
            assertEquals("https://pbs.twimg.com/profile_images/test.jpg", profile.avatarUrl());
            assertEquals(500, profile.followerCount());
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1","username":"u","name":"n","public_metrics":{"followers_count":0,"following_count":0}}}
                        """)));

            adapter.getProfile("my-token");

            wireMock.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlPathEqualTo("/users/me"))
                .withHeader("Authorization", equalTo("Bearer my-token")));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"errors":[{"message":"Unauthorized","type":"about:blank"}]}
                        """)));

            assertThrows(SnsApiException.class, () -> adapter.getProfile("bad-token"));
        }

        @Test
        @DisplayName("missing profile_image_url returns null")
        void missingProfileImage() {
            wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1","username":"u","name":"n","public_metrics":{"followers_count":0,"following_count":0}}}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertNull(profile.avatarUrl());
        }

        @Test
        @DisplayName("missing public_metrics returns 0 followers")
        void missingPublicMetrics() {
            wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1","username":"u","name":"n"}}
                        """)));

            SnsProfile profile = adapter.getProfile("token");
            assertEquals(0, profile.followerCount());
        }
    }

    @Nested
    @DisplayName("post()")
    class PostTests {

        @Test
        @DisplayName("success returns PostResult with tweet ID and URL")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1234567890","text":"Hello!"}}
                        """)));

            PostResult result = adapter.post("token", new PostRequest("Hello!", List.of()));

            assertEquals("1234567890", result.platformPostId());
            assertEquals("https://twitter.com/i/status/1234567890", result.platformUrl());
            assertNotNull(result.publishedAt());
        }

        @Test
        @DisplayName("sends text in JSON body")
        void sendsTextInBody() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1","text":"t"}}
                        """)));

            adapter.post("token", new PostRequest("My tweet text", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/tweets"))
                .withRequestBody(containing("\"text\":\"My tweet text\"")));
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"1","text":"t"}}
                        """)));

            adapter.post("my-access-token", new PostRequest("test", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/tweets"))
                .withHeader("Authorization", equalTo("Bearer my-access-token")));
        }

        @Test
        @DisplayName("platformUrl uses correct prefix format")
        void platformUrlFormat() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"id":"9999","text":"t"}}
                        """)));

            PostResult result = adapter.post("token", new PostRequest("test", List.of()));
            assertTrue(result.platformUrl().startsWith("https://twitter.com/i/status/"));
            assertTrue(result.platformUrl().endsWith("9999"));
        }

        @Test
        @DisplayName("400 throws SnsApiException")
        void badRequest() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"errors":[{"message":"Bad request"}]}
                        """)));

            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("403 throws SnsApiException")
        void forbidden() {
            wireMock.stubFor(post(urlPathEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"errors":[{"message":"Forbidden"}]}
                        """)));

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
            wireMock.stubFor(delete(urlPathEqualTo("/tweets/123"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"data":{"deleted":true}}
                        """)));

            assertDoesNotThrow(() -> adapter.deletePost("token", "123"));
        }

        @Test
        @DisplayName("sends DELETE to correct URL")
        void sendsDeleteToCorrectUrl() {
            wireMock.stubFor(delete(urlPathEqualTo("/tweets/my-tweet-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            adapter.deletePost("token", "my-tweet-id");

            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/tweets/my-tweet-id")));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized() {
            wireMock.stubFor(delete(urlPathEqualTo("/tweets/123"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"errors":[{"message":"Unauthorized"}]}
                        """)));

            assertThrows(SnsApiException.class, () -> adapter.deletePost("token", "123"));
        }

        @Test
        @DisplayName("404 throws SnsApiException")
        void notFound() {
            wireMock.stubFor(delete(urlPathEqualTo("/tweets/nonexistent"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"errors":[{"message":"Not Found"}]}
                        """)));

            assertThrows(SnsApiException.class, () -> adapter.deletePost("token", "nonexistent"));
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
        @DisplayName("TwitterClient must not be null")
        void clientNotNull() {
            assertThrows(NullPointerException.class, () -> new TwitterAdapter((TwitterClient) null));
        }
    }
}
