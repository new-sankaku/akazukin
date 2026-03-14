package com.akazukin.adapter.bluesky;

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
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
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

@DisplayName("BlueskyAdapter WireMock Tests")
class BlueskyAdapterWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private BlueskyAdapter adapter;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static String createFakeJwt(String did) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sub\":\"" + did + "\"}").getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("fakesig".getBytes());
        return header + "." + payload + "." + signature;
    }

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        adapter = new BlueskyAdapter(baseUrl, HTTP_CLIENT, OBJECT_MAPPER);
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns BLUESKY")
        void platform_returnsBluesky() {
            assertEquals(SnsPlatform.BLUESKY, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 300")
        void getMaxContentLength_returns300() {
            assertEquals(300, adapter.getMaxContentLength());
        }
    }

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("returns Bluesky app-passwords settings URL")
        void returnsSettingsUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertEquals("https://bsky.app/settings/app-passwords", url);
        }

        @Test
        @DisplayName("ignores callbackUrl parameter")
        void ignoresCallbackUrl() {
            String url1 = adapter.getAuthorizationUrl("https://one.example.com", "s1");
            String url2 = adapter.getAuthorizationUrl("https://two.example.com", "s2");
            assertEquals(url1, url2);
        }

        @Test
        @DisplayName("ignores state parameter")
        void ignoresState() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "any-state");
            assertEquals("https://bsky.app/settings/app-passwords", url);
        }
    }

    @Nested
    @DisplayName("exchangeToken()")
    class ExchangeTokenTests {

        @Test
        @DisplayName("success returns SnsAuthToken with accessJwt and refreshJwt")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "accessJwt": "access-jwt-token",
                            "refreshJwt": "refresh-jwt-token",
                            "handle": "user.bsky.social",
                            "did": "did:plc:abc123"
                        }
                        """)));

            SnsAuthToken token = adapter.exchangeToken("app-password", "user.bsky.social");

            assertNotNull(token);
            assertEquals("access-jwt-token", token.accessToken());
            assertEquals("refresh-jwt-token", token.refreshToken());
            assertNull(token.expiresAt());
            assertNull(token.scope());
        }

        @Test
        @DisplayName("sends identifier and password in request body")
        void sendsCorrectBody() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"accessJwt":"a","refreshJwt":"r","handle":"h","did":"d"}
                        """)));

            adapter.exchangeToken("my-app-password", "my-handle.bsky.social");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .withRequestBody(containing("\"identifier\":\"my-handle.bsky.social\""))
                .withRequestBody(containing("\"password\":\"my-app-password\"")));
        }

        @Test
        @DisplayName("sends correct Content-Type header")
        void sendsJsonContentType() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"accessJwt":"a","refreshJwt":"r","handle":"h","did":"d"}
                        """)));

            adapter.exchangeToken("pwd", "id");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("400 error throws SnsApiException")
        void badRequest_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("Invalid identifier or password")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("bad-pwd", "bad-id"));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("401 error throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("Unauthorized")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("pwd", "id"));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("500 error throws SnsApiException")
        void serverError_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.exchangeToken("pwd", "id"));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("response with extra fields does not fail")
        void extraFields_doNotFail() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "accessJwt":"a","refreshJwt":"r","handle":"h","did":"d",
                            "email":"test@example.com","emailConfirmed":true
                        }
                        """)));

            SnsAuthToken token = adapter.exchangeToken("pwd", "id");
            assertEquals("a", token.accessToken());
            assertEquals("r", token.refreshToken());
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("success returns new SnsAuthToken")
        void success() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "accessJwt": "new-access-jwt",
                            "refreshJwt": "new-refresh-jwt",
                            "handle": "user.bsky.social",
                            "did": "did:plc:abc123"
                        }
                        """)));

            SnsAuthToken token = adapter.refreshToken("old-refresh-jwt");

            assertNotNull(token);
            assertEquals("new-access-jwt", token.accessToken());
            assertEquals("new-refresh-jwt", token.refreshToken());
        }

        @Test
        @DisplayName("sends Bearer authorization header with refresh token")
        void sendsAuthHeader() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"accessJwt":"a","refreshJwt":"r","handle":"h","did":"d"}
                        """)));

            adapter.refreshToken("my-refresh-token");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .withHeader("Authorization", equalTo("Bearer my-refresh-token")));
        }

        @Test
        @DisplayName("expired token (401) throws SnsApiException")
        void expiredToken_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("ExpiredToken")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.refreshToken("expired-token"));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("500 error throws SnsApiException")
        void serverError_throwsSnsApiException() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.refreshToken("token"));
        }

        @Test
        @DisplayName("expiresAt and scope are null")
        void expiresAtAndScopeAreNull() {
            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"accessJwt":"a","refreshJwt":"r","handle":"h","did":"d"}
                        """)));

            SnsAuthToken token = adapter.refreshToken("refresh");
            assertNull(token.expiresAt());
            assertNull(token.scope());
        }
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("success returns SnsProfile with all fields")
        void success() {
            String did = "did:plc:abc123";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "handle": "user.bsky.social",
                            "displayName": "Test User",
                            "avatar": "https://example.com/avatar.jpg",
                            "followersCount": 42
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);

            assertNotNull(profile);
            assertEquals("user.bsky.social", profile.accountIdentifier());
            assertEquals("Test User", profile.displayName());
            assertEquals("https://example.com/avatar.jpg", profile.avatarUrl());
            assertEquals(42, profile.followerCount());
        }

        @Test
        @DisplayName("missing avatar returns null")
        void missingAvatar_returnsNull() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "handle": "user.bsky.social",
                            "displayName": "No Avatar User",
                            "followersCount": 10
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);
            assertNull(profile.avatarUrl());
        }

        @Test
        @DisplayName("zero followers returns 0")
        void zeroFollowers() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "handle": "new-user.bsky.social",
                            "displayName": "New User",
                            "followersCount": 0
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);
            assertEquals(0, profile.followerCount());
        }

        @Test
        @DisplayName("missing followersCount defaults to 0")
        void missingFollowersCount_defaultsToZero() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "handle": "user.bsky.social",
                            "displayName": "User"
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);
            assertEquals(0, profile.followerCount());
        }

        @Test
        @DisplayName("missing displayName defaults to empty string")
        void missingDisplayName_defaultsToEmpty() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "handle": "user.bsky.social"
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);
            assertEquals("", profile.displayName());
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("Invalid token")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.getProfile(fakeJwt));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Server Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.getProfile(fakeJwt));
        }

        @Test
        @DisplayName("sends Authorization header with Bearer token")
        void sendsAuthHeader() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"handle":"u","displayName":"d","followersCount":0}
                        """)));

            adapter.getProfile(fakeJwt);

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .withHeader("Authorization", equalTo("Bearer " + fakeJwt)));
        }

        @Test
        @DisplayName("profile with Japanese displayName")
        void japaneseDisplayName() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("""
                        {
                            "handle": "user.bsky.social",
                            "displayName": "\u30c6\u30b9\u30c8\u30e6\u30fc\u30b6\u30fc",
                            "followersCount": 5
                        }
                        """)));

            SnsProfile profile = adapter.getProfile(fakeJwt);
            assertEquals("\u30c6\u30b9\u30c8\u30e6\u30fc\u30b6\u30fc", profile.displayName());
        }
    }

    @Nested
    @DisplayName("post()")
    class PostTests {

        @Test
        @DisplayName("success returns PostResult with rkey and URL")
        void success() {
            String did = "did:plc:abc123";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "uri": "at://did:plc:abc123/app.bsky.feed.post/3k2abc",
                            "cid": "bafyreiabc123"
                        }
                        """)));

            PostResult result = adapter.post(fakeJwt, new PostRequest("Hello Bluesky!", List.of()));

            assertNotNull(result);
            assertEquals("3k2abc", result.platformPostId());
            assertNotNull(result.platformUrl());
            assertTrue(result.platformUrl().contains("/post/3k2abc"));
            assertNotNull(result.publishedAt());
        }

        @Test
        @DisplayName("platformUrl contains DID and rkey")
        void platformUrlFormat() {
            String did = "did:plc:xyz789";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "uri": "at://did:plc:xyz789/app.bsky.feed.post/rkey123",
                            "cid": "bafyreidef456"
                        }
                        """)));

            PostResult result = adapter.post(fakeJwt, new PostRequest("test post", List.of()));

            assertEquals("https://bsky.app/profile/" + did + "/post/rkey123", result.platformUrl());
        }

        @Test
        @DisplayName("sends correct request body with repo, collection, record")
        void sendsCorrectRequestBody() {
            String did = "did:plc:abc123";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            adapter.post(fakeJwt, new PostRequest("Hello!", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .withRequestBody(containing("\"repo\":\"" + did + "\""))
                .withRequestBody(containing("\"collection\":\"app.bsky.feed.post\""))
                .withRequestBody(containing("\"text\":\"Hello!\"")));
        }

        @Test
        @DisplayName("post with Japanese text succeeds")
        void japaneseText() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            PostResult result = adapter.post(fakeJwt,
                new PostRequest("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01", List.of()));

            assertNotNull(result);
        }

        @Test
        @DisplayName("post with max length (300 chars) succeeds")
        void maxLengthText() {
            String fakeJwt = createFakeJwt("did:plc:abc123");
            String maxText = "a".repeat(300);

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            PostResult result = adapter.post(fakeJwt, new PostRequest(maxText, List.of()));
            assertNotNull(result);
        }

        @Test
        @DisplayName("400 error throws SnsApiException")
        void badRequest_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("Invalid record")));

            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post(fakeJwt, new PostRequest("test", List.of())));
            assertEquals(SnsPlatform.BLUESKY, ex.getPlatform());
        }

        @Test
        @DisplayName("401 error throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("Auth required")));

            assertThrows(SnsApiException.class,
                () -> adapter.post(fakeJwt, new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("500 error throws SnsApiException")
        void serverError_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal error")));

            assertThrows(SnsApiException.class,
                () -> adapter.post(fakeJwt, new PostRequest("test", List.of())));
        }

        @Test
        @DisplayName("record contains createdAt field")
        void recordContainsCreatedAt() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            adapter.post(fakeJwt, new PostRequest("test", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .withRequestBody(containing("\"createdAt\":")));
        }

        @Test
        @DisplayName("record contains $type field")
        void recordContainsType() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            adapter.post(fakeJwt, new PostRequest("test", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .withRequestBody(containing("\"$type\":\"app.bsky.feed.post\"")));
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            adapter.post(fakeJwt, new PostRequest("test", List.of()));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .withHeader("Authorization", equalTo("Bearer " + fakeJwt)));
        }

        @Test
        @DisplayName("URI without slash returns full string as rkey")
        void uriWithoutSlash() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"no-slash-uri","cid":"c"}
                        """)));

            PostResult result = adapter.post(fakeJwt, new PostRequest("test", List.of()));
            assertEquals("no-slash-uri", result.platformPostId());
        }

        @Test
        @DisplayName("post with media URLs is sent (media ignored by Bluesky text post)")
        void withMediaUrls() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.createRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/k","cid":"c"}
                        """)));

            PostResult result = adapter.post(fakeJwt,
                new PostRequest("test", List.of("https://example.com/img.jpg")));
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePostTests {

        @Test
        @DisplayName("success does not throw")
        void success() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

            assertDoesNotThrow(() -> adapter.deletePost(fakeJwt, "3k2abc"));
        }

        @Test
        @DisplayName("sends correct request body with repo, collection, rkey")
        void sendsCorrectBody() {
            String did = "did:plc:abc123";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

            adapter.deletePost(fakeJwt, "my-rkey");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .withRequestBody(containing("\"repo\":\"" + did + "\""))
                .withRequestBody(containing("\"collection\":\"app.bsky.feed.post\""))
                .withRequestBody(containing("\"rkey\":\"my-rkey\"")));
        }

        @Test
        @DisplayName("401 throws SnsApiException")
        void unauthorized_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("Unauthorized")));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(fakeJwt, "rkey"));
        }

        @Test
        @DisplayName("404 throws SnsApiException")
        void notFound_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withBody("Record not found")));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(fakeJwt, "nonexistent"));
        }

        @Test
        @DisplayName("500 throws SnsApiException")
        void serverError_throwsSnsApiException() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Server Error")));

            assertThrows(SnsApiException.class,
                () -> adapter.deletePost(fakeJwt, "rkey"));
        }

        @Test
        @DisplayName("sends Authorization header")
        void sendsAuthHeader() {
            String fakeJwt = createFakeJwt("did:plc:abc123");

            wireMock.stubFor(post(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            adapter.deletePost(fakeJwt, "rkey");

            wireMock.verify(postRequestedFor(urlPathEqualTo("/xrpc/com.atproto.repo.deleteRecord"))
                .withHeader("Authorization", equalTo("Bearer " + fakeJwt)));
        }
    }

    @Nested
    @DisplayName("DID extraction from JWT")
    class DidExtractionTests {

        @Test
        @DisplayName("valid JWT extracts DID from sub claim")
        void validJwt_extractsDid() {
            String did = "did:plc:specific123";
            String fakeJwt = createFakeJwt(did);

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"handle":"u","displayName":"d","followersCount":0}
                        """)));

            adapter.getProfile(fakeJwt);

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .withQueryParam("actor", equalTo(did)));
        }

        @Test
        @DisplayName("non-JWT token uses token as-is for DID")
        void nonJwt_usesTokenAsIs() {
            String plainToken = "not-a-jwt-token";

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"handle":"u","displayName":"d","followersCount":0}
                        """)));

            adapter.getProfile(plainToken);

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .withQueryParam("actor", equalTo(plainToken)));
        }

        @Test
        @DisplayName("JWT without sub claim uses token as-is")
        void jwtWithoutSub_usesTokenAsIs() {
            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"bluesky\"}".getBytes());
            String sig = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("sig".getBytes());
            String jwtNoSub = header + "." + payload + "." + sig;

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"handle":"u","displayName":"d","followersCount":0}
                        """)));

            adapter.getProfile(jwtNoSub);

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .withQueryParam("actor", equalTo(jwtNoSub)));
        }

        @Test
        @DisplayName("JWT with empty sub claim uses token as-is")
        void jwtWithEmptySub_usesTokenAsIs() {
            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"\"}".getBytes());
            String sig = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("sig".getBytes());
            String jwtEmptySub = header + "." + payload + "." + sig;

            wireMock.stubFor(get(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"handle":"u","displayName":"d","followersCount":0}
                        """)));

            adapter.getProfile(jwtEmptySub);

            wireMock.verify(WireMock.getRequestedFor(urlPathEqualTo("/xrpc/app.bsky.actor.getProfile"))
                .withQueryParam("actor", equalTo(jwtEmptySub)));
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
        @DisplayName("serviceUrl must not be null")
        void serviceUrlNotNull() {
            assertThrows(NullPointerException.class,
                () -> new BlueskyAdapter(null, HTTP_CLIENT, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("httpClient must not be null")
        void httpClientNotNull() {
            assertThrows(NullPointerException.class,
                () -> new BlueskyAdapter("https://example.com", null, OBJECT_MAPPER));
        }

        @Test
        @DisplayName("objectMapper must not be null")
        void objectMapperNotNull() {
            assertThrows(NullPointerException.class,
                () -> new BlueskyAdapter("https://example.com", HTTP_CLIENT, null));
        }
    }
}
